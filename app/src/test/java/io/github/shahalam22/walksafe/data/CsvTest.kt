package io.github.shahalam22.walksafe.data

import io.github.shahalam22.walksafe.data.admin.toCsv
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun quotesCellsThatNeedIt() {
        val rows = listOf(
            buildJsonObject {
                put("frame_idx", 1)
                put("detail", "gap, left")
                put("reasoning", "say \"stop\"")
                put("ttc_s", JsonNull)
            },
            buildJsonObject {
                put("frame_idx", JsonPrimitive(2))
                put("detail", "ahead")
                put("reasoning", "ok")
                put("ttc_s", 1.5)
            },
        )
        assertEquals(
            "frame_idx,detail,reasoning,ttc_s\n1,\"gap, left\",\"say \"\"stop\"\"\",\n2,ahead,ok,1.5",
            toCsv(rows),
        )
    }

    @Test
    fun noRowsIsEmpty() = assertEquals("", toCsv(emptyList()))
}
