package org.bundleproject

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.charsets.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.bundleproject.json.responses.ModResponse
import org.bundleproject.utils.CannotFindTestModException
import org.bundleproject.utils.fetchAssets

class ApplicationTest {
    private val gson = Gson()

    private suspend fun getTestRoute(): String {
        val assets = fetchAssets()
        val id = assets.mods.keys.first()
        val asset = assets.mods[id]
        val platform = asset?.platforms?.keys?.first()
        val mcVer = asset?.platforms?.get(platform)?.keys?.first()
        val modVer =
            asset?.platforms?.get(platform)?.get(mcVer)?.keys?.first()
                ?: throw CannotFindTestModException()
        return "/v1/mods/$id/$platform/$mcVer/$modVer"
    }
    @Test
    fun testMod() {
        withTestApplication({ configurePlugins() }) {
            handleRequest(HttpMethod.Get, runBlocking { getTestRoute() }) {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
                .apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(
                        ContentType.Application.Json.withCharset(Charsets.UTF_8),
                        response.contentType()
                    )
                    gson.fromJson(response.content, ModResponse::class.java)
                }
        }
    }

    @Test
    fun testModDownload() {
        withTestApplication({ configurePlugins() }) {
            handleRequest(HttpMethod.Get, runBlocking { getTestRoute() } + "/download").apply {
                assertEquals(HttpStatusCode.MovedPermanently, response.status())
            }
        }
    }
}
