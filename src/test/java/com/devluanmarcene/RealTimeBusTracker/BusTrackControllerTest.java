package com.devluanmarcene.RealTimeBusTracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.devluanmarcene.RealTimeBusTracker.controller.BusTrackController;
import com.devluanmarcene.RealTimeBusTracker.dto.response.RouteResponse;
import com.devluanmarcene.RealTimeBusTracker.model.AgencyList;
import com.devluanmarcene.RealTimeBusTracker.model.Route;
import com.devluanmarcene.RealTimeBusTracker.service.BusTrackService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = BusTrackController.class, properties = {
        "routes.poll.interval=1"
})
class BusTrackControllerTest {

    @MockitoBean
    private BusTrackService busTrackService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void test_shouldReturnListOfAgencies() {
        when(busTrackService.getAgencies()).thenReturn(Mono.just(ModelBuilder.createAgencyList()));

        webTestClient.get()
                .uri("/api/bustrack/agencies")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AgencyList.class)
                .consumeWith(response -> {
                    AgencyList agencyList = response.getResponseBody();
                    assertNotNull(agencyList);
                    assertEquals(1, agencyList.agencies().size());
                    assertEquals("tag", agencyList.agencies().get(0).tag());
                });

        verify(busTrackService, times(1)).getAgencies();
    }

    @Test
    void test_shouldReturnFluxOfRoutes() {

        RouteResponse routeResponse = ModelBuilder.createRouteResponse();

        when(busTrackService.getRoutesByAgencyTagWithPredictions("agencyTag1"))
                .thenReturn(Flux.just(routeResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/bustrack/routes")
                        .queryParam("agency", "agencyTag1").build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(RouteResponse.class)
                .consumeWith(body -> {
                    List<RouteResponse> routes = body.getResponseBody().take(2).collectList().block();
                    assertNotNull(routes);
                    assertEquals(routes.size(), 2);
                    assertEquals(routes.get(0), routeResponse);
                });

        verify(busTrackService, atLeastOnce()).getRoutesByAgencyTagWithPredictions("agencyTag1");
    }

    @Test
    void test_returnShouldBeBadRequestWhenNoAgencyParameter() {
        when(busTrackService.getRoutesByAgencyTagWithPredictions("agencyTag1"))
                .thenReturn(Flux.just(ModelBuilder.createRouteResponse()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/bustrack/routes").build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        verify(busTrackService, never()).getRoutesByAgencyTagWithPredictions("agencyTag1");
    }

    @Test
    void test_shouldReturnError() {
        when(busTrackService.getRoutesByAgencyTagWithPredictions("agencyTag1"))
                .thenReturn(Flux.error(new RuntimeException("Error")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/bustrack/routes").build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        verify(busTrackService, never()).getRoutesByAgencyTagWithPredictions("agencyTag1");
    }

    @Test
    void test_shouldStopSendindEventsAfterSSEConnectionClose() {
        when(busTrackService.getRoutesByAgencyTagWithPredictions("agencyTag1"))
                .thenReturn(Flux.just(ModelBuilder.createRouteResponse()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/bustrack/routes")
                        .queryParam("agency", "agencyTag1").build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(Route.class)
                .consumeWith(responseBody -> {
                    Route route = responseBody.getResponseBody().take(1).blockFirst();
                    assertNotNull(route);
                });

        verify(busTrackService, times(1)).getRoutesByAgencyTagWithPredictions("agencyTag1");
        verifyNoMoreInteractions(busTrackService);
    }

}
