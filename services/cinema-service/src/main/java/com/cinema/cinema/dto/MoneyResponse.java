package com.cinema.cinema.dto; // 7.9

public record MoneyResponse(long amountMinor, String currency) { // 7.10 Tiền theo đơn vị nhỏ nhất, khớp {amountMinor,currency}
}