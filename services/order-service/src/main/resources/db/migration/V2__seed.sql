-- Seed VAT 10% + 2 voucher mẫu (plan §6.5).
INSERT INTO tax_config (code, percent) VALUES ('VAT', 10.00)
ON CONFLICT (code) DO NOTHING;

INSERT INTO vouchers (code, type, value, min_order_value, max_discount, expires_at, usage_limit)
VALUES
    ('WELCOME10', 'PERCENT', 10.00, 1000000, 500000, now() + interval '30 days', 1000),
    ('SUMMER200K', 'FIXED', 200000, 2000000, NULL, now() + interval '30 days', 500)
ON CONFLICT (code) DO NOTHING;
