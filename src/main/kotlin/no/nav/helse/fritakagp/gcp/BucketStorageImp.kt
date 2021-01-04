package no.nav.helse.fritakagp.gcp

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Blob
import java.nio.ByteBuffer
import com.google.cloud.storage.BlobId
import java.util.*

interface BucketStorage {
    fun uploadDoc(uuid: UUID, filContent : String, filExt : String)
    fun getDocAsString(uuid: String, path: String) : BucketDocument
    fun deleteDoc(uuid : String)
}
data class BucketDocument(val base64Data :String, val extension: String)

class MockBucketStorage() : BucketStorage {
    private  val items = HashMap<String, BucketDocument>()
    private val default = BucketDocument("default doc","txt")
    override fun uploadDoc(uuid: UUID, filContent : String, filExt : String) {
        items[uuid.toString()] = BucketDocument(filContent,filExt)
    }

    override fun getDocAsString(uuid: String, path: String) : BucketDocument {
        return items.getOrDefault(uuid, default)
    }

    override fun deleteDoc(uuid : String) {
        items.remove(uuid)
    }
}

class BucketStorageImp(private val bucketName : String = "fritakagb-bucket"): BucketStorage  {
    private val storage: Storage = StorageOptions.getDefaultInstance().service
    private val bucket : Bucket = storage.get(bucketName) ?: error("Bucket $bucketName eksistere ikke")

    override fun uploadDoc(uuid: UUID, filContent : String, filExt : String) {
        val blob = createBlob(bucketName, uuid, filExt)
        blob.writer().use {
            writer -> writer.write(ByteBuffer.wrap(filContent.toByteArray()))
        }
    }

    override fun getDocAsString(uuid: UUID, path: String) : BucketDocument {
        val blob = bucket.get(uuid.toString()) ?: error("Object $uuid eksistere ikke")
        val blobMeta = blob.metadata
        val ext = blobMeta["ext"] ?: ""

        return BucketDocument(blob.getContent().toString(), ext)
    }


    override fun deleteDoc(uuid : UUID) {
        val blob = bucket.get(uuid.toString()) ?: error("Object/fil $uuid eksistere ikke")
        blob.delete()
    }

    private fun createBlob(bucketName: String, blobName: UUID, ext : String): Blob {
        val blobMetadata: MutableMap<String, String> = HashMap()
        blobMetadata["ext"] = ext
        val blobId = BlobId.of(bucketName, blobName.toString())
        val blobInfo = BlobInfo.newBuilder(blobId).setMetadata(blobMetadata).build()

        return storage.create(blobInfo)
    }
}
