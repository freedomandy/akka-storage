# akka-storage
Storage restful api example based on Akka-Http and Azure Storage

_Bolb API_
1. **Upload blob(Chunk upload)**: curl -X POST http://127.0.0.1:8080/apis/v1/container/{containerName}/blob -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' -F file=@{uploadFile}
2. **List blobs by giving specified container**: curl -X GET http://127.0.0.1:8080/apis/v1/container/{containerName}/blobs
3. **Download blob**: curl -X GET http://127.0.0.1:8080/apis/v1/container/{containerName}/blob/{blobName}
4. **Delete blob**: curl -X DELETE http://127.0.0.1:8080/apis/v1/container/{containerName}/blob/{blobName}

_Container API_
1. **Create container**: curl -X POST http://127.0.0.1:8080/apis/v1/container/{containerName}
2. **List containers**: curl -X GET http://127.0.0.1:8080/apis/v1/containers
3. **Delete container**: curl -X DELETE http://127.0.0.1:8080/apis/v1/container/{containerName}
