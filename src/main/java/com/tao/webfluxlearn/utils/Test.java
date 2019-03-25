package com.tao.webfluxlearn.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.elasticsearch.common.StopWatch;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.xml.bind.SchemaOutputResolver;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        HttpClient httpClient = HttpClient.create()
                .baseUrl("http://localhost:8109")
                .tcpConfiguration(client -> client
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                        .doOnConnected(conn -> conn
                                .addHandlerLast(new ReadTimeoutHandler(10))
                                .addHandlerLast(new WriteTimeoutHandler(10)))
                );
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();


        ArrayList<Mono<JsonNode>> monos = new ArrayList<>();
        ArrayList<Long> starts = new ArrayList<>();
        ArrayList<Long> stops = new ArrayList<>();
        WebClient webClient1 = WebClient.create();
        for (int i = 0; i <10; i++) {
            StopWatch stopWatch = new StopWatch("req " + i);
            stopWatch.start();
            Mono<JsonNode> mono = webClient1.get()
                    .uri("http://localhost:8109/health_chk")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doAfterSuccessOrError(new BiConsumer<JsonNode, Throwable>() {
                        @Override
                        public void accept(JsonNode jsonNode, Throwable throwable) {
                            stopWatch.stop();
                            System.out.println(stopWatch.toString()+stopWatch.lastTaskTime());
                        }
                    });
            monos.add(mono);
        }
        Flux.merge(monos).subscribe();

        TimeUnit.SECONDS.sleep(1L);
        long t1 = System.nanoTime();
        JsonNode block = WebClient.create().get().uri("http://localhost:8109/health_chk").retrieve().bodyToMono(JsonNode.class).block();
        long t2 = System.nanoTime();
        System.out.println("single req cost time "+(t2-t1));

        System.out.println();
    }
}
