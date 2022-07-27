// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.firestore

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.ktx.toObject
import kotlinx.serialization.Serializable
import org.junit.Test

class DocumentIdIntegrationTests {

    @Serializable
    private data class DocumentIdOnDocRefFieldWithAnnotation(
        @DocumentId val docId: DocumentReference? = null,
        @DocumentId val stringDocId: String? = null
    )

    @Test
    fun decoding_DocumentReference_with_KDocumentId_annotation_is_supported() {
        val docRefKotlin = testCollection("ktx").document("123")
        val docRefObjectList =
            listOf(
                DocumentIdOnDocRefFieldWithAnnotation(),
                DocumentIdOnDocRefFieldWithAnnotation(docRefKotlin),
                DocumentIdOnDocRefFieldWithAnnotation(stringDocId = "foo/bar"),
                DocumentIdOnDocRefFieldWithAnnotation(docRefKotlin, "foo/bar")
            )

        for (obj in docRefObjectList) {
            docRefKotlin.set(obj)
            val actual =
                waitFor(docRefKotlin.get()).toObject<DocumentIdOnDocRefFieldWithAnnotation>()
            val expected =
                waitFor(docRefKotlin.get()).withoutCustomMappers {
                    toObject<DocumentIdOnDocRefFieldWithAnnotation>()
                }
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Serializable
    private data class DocumentIdOnDocRefField(
        @DocumentId val docRef: DocumentReference? = null,
        @DocumentId val docRefStr: String? = null
    )
    @Test
    fun ktx_resolved_documentReference_is_equivalent_to_java() {
        val docRefKotlin = testCollection("ktx").document("123")
        val docRefObject = DocumentIdOnDocRefField()
        docRefKotlin.set(docRefObject)
        val actual = waitFor(docRefKotlin.get()).data

        docRefKotlin.withoutCustomMappers { set(docRefObject) }
        val expected = waitFor(docRefKotlin.get()).data

        // encoding are the same
        assertThat(actual).containsExactlyEntriesIn(expected)

        val expectedObj =
            waitFor(docRefKotlin.get()).withoutCustomMappers { toObject<DocumentIdOnDocRefField>() }
        val actualObj = waitFor(docRefKotlin.get()).toObject<DocumentIdOnDocRefField>()
        // decoding are the same
        assertThat(actualObj).isEqualTo(expectedObj)
    }

    @Serializable
    private data class DocumentIdOnStringField(@DocumentId val docId: String? = "doc-id")

    @Serializable
    private data class DocumentIdOnNestedObjects(
        var nested: DocumentIdOnStringField = DocumentIdOnStringField()
    )

    @Test
    fun documentId_annotation_works_on_nested_object() {
        val docRefPOJO = testCollection("pojo").document("java_kxt_same_docRef_str")
        val docRefKotlin = testCollection("ktx").document("java_kxt_same_docRef_str")
        docRefKotlin.set(DocumentIdOnNestedObjects())
        docRefPOJO.withoutCustomMappers { set(DocumentIdOnNestedObjects()) }
        val expected =
            waitFor(docRefPOJO.get()).withoutCustomMappers { toObject<DocumentIdOnNestedObjects>() }
        val actual = waitFor(docRefKotlin.get()).toObject<DocumentIdOnNestedObjects>()
        assertThat(actual).isEqualTo(expected)
    }


    data class JavaDocIdTest(
        val value1: String = "should be kept",
        @DocumentId
        val value2: String = "should be ignored"
    )
    @Test
    fun java_annotation_and_ktx_annotation_should_work_together() {
        // DocumentId annotated field will be ignored in encoding
        val javaDocumentId = JavaDocIdTest()
        val docRefPOJO = testCollection("pojo").document("java_kxt_same_docRef_str")
        docRefPOJO.set(javaDocumentId)
        val actual = waitFor(docRefPOJO.get()).data
        val expected = mutableMapOf("value1" to "should be kept", "value2" to "java_kxt_same_docRef_str")
        assertThat(actual).isEqualTo(expected)
    }
}