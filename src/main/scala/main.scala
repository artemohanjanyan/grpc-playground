import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import io.grpc.reflection.v1alpha.{ServerReflectionGrpc, ServerReflectionRequest, ServerReflectionResponse}
import io.grpc.stub.{ClientCalls, StreamObserver}
import io.grpc.{CallOptions, Channel, ClientCall, ClientInterceptor, ClientInterceptors, ForwardingClientCall, ManagedChannel, ManagedChannelBuilder, Metadata, MethodDescriptor}

import java.util.Base64
import scala.jdk.CollectionConverters._
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.*

class BinaryHeaderClientInterceptor(headerKey: String, headerValue: Array[Byte]) extends ClientInterceptor:
  private val key: Metadata.Key[Array[Byte]] =
    Metadata.Key.of(headerKey, Metadata.BINARY_BYTE_MARSHALLER)

  override def interceptCall[ReqT, RespT](method: MethodDescriptor[ReqT, RespT],
                                          callOptions: CallOptions,
                                          next: Channel,
                                         ): ClientCall[ReqT, RespT] =
    new ForwardingClientCall.SimpleForwardingClientCall[ReqT, RespT](
      next.newCall(method, callOptions)
    ):
      override def start(responseListener: ClientCall.Listener[RespT], headers: Metadata): Unit =
        headers.put(key, headerValue)
        super.start(responseListener, headers)

@main
def main(backendAuthority: String,
         serviceName: String,
         ip: String,
         header: String,
        ): Unit =
  val channel: Channel = ClientInterceptors.intercept(
    ManagedChannelBuilder
      .forAddress(ip, 8081)
      .usePlaintext()
      .build(),
    new BinaryHeaderClientInterceptor(
      "wix-request-context-bin",
      Base64.getDecoder.decode(header),
    ),
  )

  object responseObserver extends StreamObserver[ServerReflectionResponse]:
    val requestObserver: StreamObserver[ServerReflectionRequest] = ClientCalls.asyncBidiStreamingCall(
      channel.newCall(ServerReflectionGrpc.getServerReflectionInfoMethod, CallOptions.DEFAULT.withAuthority(backendAuthority)),
      this,
    )

    val result: Promise[List[FileDescriptorProto]] = Promise[List[FileDescriptorProto]]()

    override def onNext(value: ServerReflectionResponse): Unit =
      if value.hasFileDescriptorResponse then
        val files = value.getFileDescriptorResponse.getFileDescriptorProtoList.asScala.toList
        val fileDescriptorProtos = files.map(FileDescriptorProto.parseFrom)
        requestObserver.onCompleted()
        result.success(fileDescriptorProtos)
      else
        throw new RuntimeException(":((((")

    override def onError(t: Throwable): Unit =
      requestObserver.onError(t)
      result.failure(t)

    override def onCompleted(): Unit = {}

  responseObserver.requestObserver.onNext(
    ServerReflectionRequest.newBuilder()
      .setFileContainingSymbol(serviceName)
      .build()
  )
  val fileDescriptorProtos: List[FileDescriptorProto] =
    Await.result(responseObserver.result.future, 10.minutes)

  println(s"Received ${fileDescriptorProtos.size} file descriptor(s).")
  for file <- fileDescriptorProtos do
    println(file.getName + ", dependencies (not necessarily included):")
    for dependency <- file.getDependencyList.asScala do
      println(s"  - $dependency")