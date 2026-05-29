-- ============================================================
-- Identity Service — seed dữ liệu demo
-- LƯU Ý: password_hash để NULL ở đây. DataSeeder (Java) sẽ set
-- bcrypt hash của mật khẩu mặc định "Demo@123" khi startup nếu NULL.
-- ============================================================

INSERT INTO branches (id, code, name, address, city, district, ward, phone, lat, lng) VALUES
('11111111-1111-1111-1111-111111111111', 'HCM', 'THLuxury Quận 1',
    '101 Đồng Khởi', 'Hồ Chí Minh', 'Quận 1', 'Bến Nghé', '02838221001', 10.776530, 106.703690),
('22222222-2222-2222-2222-222222222222', 'HN', 'THLuxury Hoàn Kiếm',
    '25 Tràng Tiền',  'Hà Nội',     'Hoàn Kiếm', 'Tràng Tiền', '02438221002', 21.024030, 105.852000),
('33333333-3333-3333-3333-333333333333', 'DN', 'THLuxury Hải Châu',
    '180 Bạch Đằng',  'Đà Nẵng',    'Hải Châu',  'Hải Châu 1', '02363821003', 16.069430, 108.221450)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (id, email, password_hash, full_name, phone, role, branch_id) VALUES
('aaaaaaaa-0000-0000-0000-000000000001',
    'admin@thluxury.local',           NULL, 'System Admin',   '0900000001', 'ADMIN', NULL),

('bbbbbbbb-0000-0000-0000-000000000001',
    'manager.hcm@thluxury.local',     NULL, 'Quản lý HCM',    '0911100001', 'BRANCH_MANAGER',
    '11111111-1111-1111-1111-111111111111'),
('bbbbbbbb-0000-0000-0000-000000000002',
    'manager.hn@thluxury.local',      NULL, 'Quản lý HN',     '0911100002', 'BRANCH_MANAGER',
    '22222222-2222-2222-2222-222222222222'),
('bbbbbbbb-0000-0000-0000-000000000003',
    'manager.dn@thluxury.local',      NULL, 'Quản lý ĐN',     '0911100003', 'BRANCH_MANAGER',
    '33333333-3333-3333-3333-333333333333'),

('cccccccc-0000-0000-0000-000000000001',
    'an.nguyen@example.com',          NULL, 'Nguyễn Văn An',  '0901234567', 'CUSTOMER', NULL),
('cccccccc-0000-0000-0000-000000000002',
    'binh.tran@example.com',          NULL, 'Trần Thị Bình',  '0912345678', 'CUSTOMER', NULL)
ON CONFLICT (email) DO NOTHING;
