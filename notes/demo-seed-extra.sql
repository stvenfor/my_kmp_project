-- 前端演示数据补齐（幂等）：钱包 / 银行卡 / 流水 / 收货地址 / 售后工单 / 商城订单 / 门店统计
-- 目标用户：13400000000（dev 测试号）
BEGIN;

-- 0) 解析测试用户
CREATE TEMP TABLE _seed_user AS
SELECT user_id FROM users WHERE phone = '13400000000' LIMIT 1;

-- 1) 门店统计（“我的”页四指标）
INSERT INTO wys_user_store_stats
  (user_id, store_id, store_name, role, days_joined, employee_count, store_days, total_customers, created_at, updated_at)
SELECT u.user_id, 1, '[4S]北京沃德龙鼎吉利', 2, 365, 28, 2059, 9366, now(), now()
FROM _seed_user u
ON CONFLICT (user_id, store_id) DO UPDATE SET
  store_name = EXCLUDED.store_name,
  role = EXCLUDED.role,
  days_joined = EXCLUDED.days_joined,
  employee_count = EXCLUDED.employee_count,
  store_days = EXCLUDED.store_days,
  total_customers = EXCLUDED.total_customers,
  updated_at = now();

-- 2) 现金钱包余额（843.00 元）
INSERT INTO wys_cash_wallet (user_id, balance_fen, updated_at)
SELECT u.user_id, 84300, now() FROM _seed_user u
ON CONFLICT (user_id) DO UPDATE SET balance_fen = EXCLUDED.balance_fen, updated_at = now();

-- 3) 银行卡（两张，其中一张默认）
INSERT INTO wys_bank_card (user_id, bank_name, card_last4, holder_name, is_default, created_at, updated_at)
SELECT u.user_id, v.bank_name, v.card_last4, v.holder_name, v.is_default, now() - v.age, now() - v.age
FROM _seed_user u
CROSS JOIN (VALUES
  ('招商银行', '6688', '测试甲', true,  interval '120 day'),
  ('中国工商银行', '1024', '测试甲', false, interval '60 day')
) AS v(bank_name, card_last4, holder_name, is_default, age)
WHERE NOT EXISTS (
  SELECT 1 FROM wys_bank_card c WHERE c.user_id = u.user_id AND c.card_last4 = v.card_last4
);

-- 4) 钱包流水（余额末值 = 84300，与钱包一致）
INSERT INTO wys_cash_ledger (user_id, delta_fen, balance_fen, reason, ref_id, created_at)
SELECT u.user_id, v.delta, v.balance, v.reason, v.ref_id, now() - v.age
FROM _seed_user u
CROSS JOIN (VALUES
  (100000::bigint, 100000::bigint, 'recharge',  'seed-demo-recharge-1', interval '60 day'),
  (-25600::bigint,  74400::bigint, 'mall_pay',  'seed-demo-mall-pay-1', interval '30 day'),
  ( 9900::bigint,   84300::bigint, 'mall_refund','seed-demo-refund-1',   interval '25 day')
) AS v(delta, balance, reason, ref_id, age)
WHERE NOT EXISTS (
  SELECT 1 FROM wys_cash_ledger l WHERE l.user_id = u.user_id AND l.ref_id = v.ref_id
);

-- 5) 收货地址（三条，一条默认）
INSERT INTO wys_user_address
  (user_id, receiver_name, receiver_phone, province, city, district, detail_address, postal_code, is_default, label, created_at, updated_at)
SELECT u.user_id, v.name, v.phone, v.province, v.city, v.district, v.detail, '', v.is_default, v.label, now() - v.age, now() - v.age
FROM _seed_user u
CROSS JOIN (VALUES
  ('测试甲', '13800115172', '北京市', '北京市', '朝阳区', '演示路 1 号 4S 店展厅', true,  '公司', interval '90 day'),
  ('李婉婷', '13900139000', '北京市', '北京市', '海淀区', '中关村大街 18 号 3 单元 602', false, '家',   interval '40 day'),
  ('王建国', '13700137000', '上海市', '上海市', '浦东新区', '世纪大道 100 号 12 层', false, '',     interval '15 day')
) AS v(name, phone, province, city, district, detail, is_default, label, age)
WHERE NOT EXISTS (
  SELECT 1 FROM wys_user_address a
  WHERE a.user_id = u.user_id AND a.receiver_phone = v.phone AND a.detail_address = v.detail AND a.deleted_at IS NULL
);

-- 6) 售后工单（门店可见列表）
INSERT INTO wys_after_sales_record
  (store_id, appointment_id, customer_id, customer_user_id, customer_name, customer_phone, plate_no, mileage,
   service_kind, title, content, service_date, created_by, created_at, updated_at)
SELECT 1, NULL, v.customer_id, u.user_id, v.name, v.phone, v.plate, v.mileage, v.kind, v.title, v.content,
       current_date - v.days_ago, u.user_id, now() - (v.days_ago || ' day')::interval, now() - (v.days_ago || ' day')::interval
FROM _seed_user u
CROSS JOIN (VALUES
  (39::bigint, '王建国', '13700137000', '京A·8Z6Q1', 36800::bigint, 1::smallint, '常规保养', '更换机油机滤，检查刹车片与轮胎气压。', 10),
  (40::bigint, '李婉婷', '13900139000', '京N·5T2M8', 12400::bigint, 2::smallint, '钣金喷漆', '右前叶子板轻微剐蹭，做局部补漆。', 6),
  (41::bigint, '待跟进客户-3', '13600136000', '京C·9K1L2', 55200::bigint, 1::smallint, '大保养', '更换三滤与火花塞，清洗节气门。', 3)
) AS v(customer_id, name, phone, plate, mileage, kind, title, content, days_ago)
WHERE NOT EXISTS (
  SELECT 1 FROM wys_after_sales_record r
  WHERE r.store_id = 1 AND r.plate_no = v.plate AND r.title = v.title
);

-- 7) 商城订单（两笔：已支付 / 待支付）+ 订单项
INSERT INTO wys_mall_order
  (order_no, store_id, buyer_user_id, idempotency_key, status, payment_channel, amount,
   receiver_name, receiver_phone, receiver_address, paid_at, created_at, updated_at, total_points, payment_mode)
SELECT v.order_no, 1, u.user_id, v.idem, v.status, v.channel, v.amount,
       '测试甲', '13800115172', '北京市朝阳区演示路 1 号 4S 店展厅', v.paid_at,
       now() - v.age, now() - v.age, 0, 1
FROM _seed_user u
CROSS JOIN (VALUES
  ('MO-SEED-1001', 'seed-demo-order-1001', 2::smallint, 1::smallint, 79.80::numeric, now() - interval '9 day',  interval '9 day'),
  ('MO-SEED-1002', 'seed-demo-order-1002', 0::smallint, NULL::smallint, 128.00::numeric, NULL::timestamptz, interval '0 day')
) AS v(order_no, idem, status, channel, amount, paid_at, age)
WHERE NOT EXISTS (
  SELECT 1 FROM wys_mall_order o WHERE o.order_no = v.order_no
);

INSERT INTO wys_mall_order_item
  (order_id, sku_id, product_id, kind, product_title, cover_url, specs, price, qty, line_amount, created_at, price_points, line_points, returned_qty)
SELECT o.order_id, v.sku_id, v.product_id, 0, v.title, NULL, '{}'::jsonb, v.price, v.qty, v.line, o.created_at, 0, 0, 0
FROM wys_mall_order o
JOIN _seed_user u ON o.buyer_user_id = u.user_id
JOIN (VALUES
  ('MO-SEED-1001', 1::bigint, 1::bigint, '店庆纪念马克杯', 39.90::numeric, 2,  79.80::numeric),
  ('MO-SEED-1002', 5::bigint, 5::bigint, '冬季保暖围巾',  128.00::numeric, 1, 128.00::numeric)
) AS v(order_no, sku_id, product_id, title, price, qty, line) ON v.order_no = o.order_no
WHERE NOT EXISTS (
  SELECT 1 FROM wys_mall_order_item i WHERE i.order_id = o.order_id AND i.sku_id = v.sku_id
);

-- 8) 积分钱包余额（签到页/积分商城不空）
INSERT INTO wys_points_wallet (user_id, balance, updated_at)
SELECT u.user_id, 1280, now() FROM _seed_user u
ON CONFLICT (user_id) DO UPDATE SET balance = GREATEST(wys_points_wallet.balance, 1280), updated_at = now();

COMMIT;
