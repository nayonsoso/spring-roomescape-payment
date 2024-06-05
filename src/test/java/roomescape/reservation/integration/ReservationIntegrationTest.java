package roomescape.reservation.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseCookie;
import roomescape.auth.domain.Token;
import roomescape.auth.provider.CookieProvider;
import roomescape.client.payment.TossPaymentClient;
import roomescape.client.payment.dto.PaymentConfirmationFromTossDto;
import roomescape.client.payment.dto.PaymentConfirmationToTossDto;
import roomescape.model.IntegrationTest;
import roomescape.registration.domain.reservation.dto.ReservationRequest;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReservationIntegrationTest extends IntegrationTest {

    @MockBean
    private TossPaymentClient tossPaymentClient;

    @Test
    @DisplayName("정상적인 요청에 대하여 예약을 정상적으로 등록, 조회, 삭제한다.")
    void adminReservationPageWork() {
        Token token = tokenProvider.getAccessToken(1);
        ResponseCookie cookie = CookieProvider.setCookieFrom(token);
        ReservationRequest reservationRequest = new ReservationRequest(TODAY.plusDays(1), 1L, 1L,
                "paymentType", "paymentKey", "orderId", 1000);
        PaymentConfirmationToTossDto paymentConfirmationToTossDto = PaymentConfirmationToTossDto.from(reservationRequest);
        given(tossPaymentClient.sendPaymentConfirm(paymentConfirmationToTossDto))
                .willReturn(getValidPaymentConfirmationFromTossDto());

        int id = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .cookie(cookie.toString())
                .body(reservationRequest)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201)
                .extract().path("id");

        RestAssured.given().log().all()
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(6));

        RestAssured.given().log().all()
                .when().delete("/reservations/" + id)
                .then().log().all()
                .statusCode(204);

        RestAssured.given().log().all()
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(5));
    }

    @Test
    @DisplayName("예약을 요청시 존재하지 않은 예약 시간의 id일 경우 예외가 발생한다.")
    void notExistTime() {
        Token token = tokenProvider.getAccessToken(1);
        ResponseCookie cookie = CookieProvider.setCookieFrom(token);
        ReservationRequest reservationRequest = new ReservationRequest(TODAY.plusDays(1), 1L, 0L,
                "paymentType", "paymentKey", "orderId", 1000);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .cookie(cookie.toString())
                .body(reservationRequest)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("모든 예약 시간 정보를 조회한다.")
    void findReservationTimeList() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .when().get("/reservations/1?date=" + TODAY)
                .then().log().all()
                .statusCode(200);
    }

    private PaymentConfirmationFromTossDto getValidPaymentConfirmationFromTossDto() {
        return new PaymentConfirmationFromTossDto(
                "test-payment-key", "test-order-id", 10000L, "DONE",
                LocalDateTime.now().plusDays(7L)
        );
    }
}
