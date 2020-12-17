package no.nav.helse.fritakagp.gcp

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Blob
import java.nio.ByteBuffer
import com.google.cloud.storage.BlobId
import java.util.HashMap


class BucketUtils(private val BUCKET_NAME : String = "fritakagb-bucket") {
    private val storage: Storage = StorageOptions.getDefaultInstance().service
    private val bucket : Bucket = storage.get(BUCKET_NAME) ?: error("Bucket $BUCKET_NAME eksistere ikke")

    fun uploadDoc(uuid: String, filContent : String, filExt : String) {
        val blob = createBlob(BUCKET_NAME, uuid, filExt)
        blob.writer().use {
            writer -> writer.write(ByteBuffer.wrap(filContent.toByteArray()))
        }
    }

    fun getDocAsString(uuid: String, path: String) : Pair<String,String> {
        val blob = bucket.get(uuid) ?: error("Object $uuid eksistere ikke")
        val blobMeta = blob.metadata
        val ext = blobMeta["ext"] ?: ""

        return Pair(blob.getContent().toString(), ext)
    }


    fun deleteDoc(uuid : String) {
        val blob = bucket.get(uuid) ?: error("Object/fil $uuid eksistere ikke")
        blob.delete()
    }

    private fun createBlob(bucketName: String, blobName: String, ext : String): Blob {
        val blobMetadata: MutableMap<String, String> = HashMap()
        blobMetadata["ext"] = ext
        val blobId = BlobId.of(bucketName, blobName)
        val blobInfo = BlobInfo.newBuilder(blobId).setMetadata(blobMetadata).build()

        return storage.create(blobInfo)
    }
}
