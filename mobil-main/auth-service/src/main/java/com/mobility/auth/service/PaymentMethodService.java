package com.mobility.auth.service;

import com.mobility.auth.dto.PaymentMethodRequest;
import com.mobility.auth.dto.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {

    List<PaymentMethodResponse> list(String userUid);

    PaymentMethodResponse add(String userUid, PaymentMethodRequest req);

    void delete(String userUid, Long methodId);

    PaymentMethodResponse setDefault(String userUid, Long methodId);
}
