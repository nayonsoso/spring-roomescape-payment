package roomescape.member.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import roomescape.auth.domain.Token;
import roomescape.auth.provider.CookieProvider;
import roomescape.auth.provider.model.TokenProvider;
import roomescape.member.domain.Member;
import roomescape.member.domain.MemberRole;
import roomescape.member.repository.MemberRepository;
import roomescape.model.IntegrationTest;
import roomescape.payment.domain.Payment;
import roomescape.payment.repository.PaymentRepository;
import roomescape.registration.domain.reservation.domain.Reservation;
import roomescape.registration.domain.reservation.repository.ReservationRepository;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationtime.repository.ReservationTimeRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.repository.ThemeRepository;
import roomescape.vo.Name;

import java.time.LocalDate;
import java.time.LocalTime;

class MemberControllerTest extends IntegrationTest {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private final int ITERATIONS = 5000;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < ITERATIONS; i++) {
            String var = "" + i;
            Theme theme = new Theme(new Name(var), var, var, (long) i);
            themeRepository.save(theme);

            ReservationTime reservationTime = new ReservationTime(LocalTime.now().plusSeconds((long) i));
            reservationTimeRepository.save(reservationTime);

            Member member = new Member(new Name(var), var + "@email.com", var + "password", MemberRole.MEMBER);
            memberRepository.save(member);

            Reservation reservation = new Reservation(LocalDate.now().plusDays(i), reservationTime, theme, member);
            reservationRepository.save(reservation);

            Payment payment = new Payment(var, var, reservation);
            paymentRepository.save(payment);
        }
    }

    @Test
    void test() throws InterruptedException {
        int memberId = 1;

        Token token = tokenProvider.getAccessToken(memberId);
        ResponseCookie cookie = CookieProvider.setCookieFrom(token);

        long start1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            RestAssured.given()
                    .cookie(cookie.toString())
                    .contentType(ContentType.JSON)
                    .when().get("/member/registrations/multiple-queries");
        }
        long end1 = System.nanoTime();
        long time1 = end1 - start1;
        System.out.println("=======");
        System.out.println("iteration : " + ITERATIONS);
        System.out.println("multiple-queries total time : " + time1 / 1000000 + "ms");

        long start2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            RestAssured.given()
                    .cookie(cookie.toString())
                    .contentType(ContentType.JSON)
                    .when().get("/member/registrations/multiple-joins");
        }
        long end2 = System.nanoTime();
        long time2 = end2 - start2;
        System.out.println("multiple-joins time : " + time2 / 1000000 + "ms");
        System.out.println((time1-time2 > 0 ? "multiple-joins" : "multiple-queries") + " is faster by " + Math.abs(time2- time1) / 1000000 + "ms");
        System.out.println("=======");
    }
}
