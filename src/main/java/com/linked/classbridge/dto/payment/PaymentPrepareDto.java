package com.linked.classbridge.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class PaymentPrepareDto {
    @Getter
    @Setter
    public static class Request {
        private String partnerOrderId;      // 가맹점 주문 번호
        private String partnerUserId;       // 가맹점 회원 id
        @JsonProperty("item_name")
        private String itemName;            // 상품명
        private int quantity;               // 상품 수량
        @JsonProperty("total_amount")
        private int totalAmount;            // 상품 총액
        private int texFreeAmount;          // 상품 비과세 금액
        private String pgToken;
        private String cid;
        private String tid;
        @JsonProperty("reservation_id")
        private Long reservationId;
    }

    @Getter
    @Setter
    public static class Response {
        private String cid;
        private String partnerOrderId;      // 가맹점 주문 번호
        private String partnerUserId;       // 가맹점 회원 id
        private String tid;                 // 결제 고유 번호
        private String next_redirect_pc_url;// pc 웹일 경우 받는 결제 페이지
        private String created_at;
        private String pgToken;
        private String itemName;            // 상품명
        private int quantity;               // 상품 수량
        private Long reservationId;
    }

    @Getter
    @Setter
    @Builder
    public static class PayResponse {
        private String next_redirect_pc_url;
        private String itemName;
        private int quantity;
        private Long reservationId;
    }
    public static PayResponse from(PaymentPrepareDto.Response response) {
        return PayResponse.builder()
                .next_redirect_pc_url(response.getNext_redirect_pc_url())
                .itemName(response.getItemName())
                .quantity(response.getQuantity())
                .reservationId(response.getReservationId())
                .build();
    }
}
