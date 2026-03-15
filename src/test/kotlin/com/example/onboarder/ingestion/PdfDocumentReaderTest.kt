package com.example.onboarder.ingestion

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfDocumentReaderTest {

    private val reader = PdfDocumentReader()

    @Test
    fun `should read valid PDF document`() {
        val resource = ClassPathResource("company_policy.pdf")
        val documents = reader.read(resource)
        
        assertNotNull(documents)
        assertTrue(documents.isNotEmpty())
    }

    @Test
    fun `should throw exception for non-existent file`() {
        val resource = FileSystemResource(File("nonexistent.pdf"))
        
        assertThrows<Exception> {
            reader.read(resource)
        }
    }

    @Test
    fun `should return documents with content`() {
        val resource = ClassPathResource("company_policy.pdf")
        val documents = reader.read(resource)
        
        documents.forEach { doc ->
            assertNotNull(doc.text)
        }
    }
}