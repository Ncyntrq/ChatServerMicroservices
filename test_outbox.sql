INSERT INTO outbox_messages (aggregate_type, aggregate_id, exchange, routing_key, payload, status, created_at) 
VALUES ('TEST', '1', 'chat.fanout', '', '{"test": "hello"}', 'PENDING', NOW());
