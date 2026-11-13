package com.cinema.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatalogClientsTest {
    @Test
    void movieFiltersAreEncodedAsQueryValuesAndCannotInjectOtherParameters() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://movie-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://movie-service/api/v1/movies?status=COMING_SOON&query=A%26B&genre=Horror"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(new MovieCatalogClient(builder.build()).listPublishedMovies("COMING_SOON", "A&B", "Horror")).isEmpty();
        server.verify();
    }

    @Test
    void cinemaAndScheduleUseOnlyPublicGetRoutesAndDecodeActualIdsAndInstants() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://cinema-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID movieId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        server.expect(requestTo("http://cinema-service/api/v1/cinemas"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://cinema-service/api/v1/showtimes?date=2026-11-14&movieId=" + movieId))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("""
                        [{"id":"%s","movieId":"%s","cinemaId":"%s",
                          "startsAt":"2026-11-14T13:00:00Z","endsAt":"2026-11-14T15:00:00Z",
                          "price":{"amountMinor":100000,"currency":"VND"},"status":"PUBLISHED"}]
                        """.formatted(showtimeId, movieId, cinemaId), MediaType.APPLICATION_JSON));
        CinemaScheduleClient client = new CinemaScheduleClient(builder.build());
        assertThat(client.listCinemas(null)).isEmpty();
        var schedule = client.listPublishedShowtimes(LocalDate.of(2026, 11, 14), movieId);
        assertThat(schedule).hasSize(1);
        assertThat(schedule.getFirst().id()).isEqualTo(showtimeId);
        assertThat(schedule.getFirst().price().amountMinor()).isEqualTo(100000);
        server.verify();
    }
}
