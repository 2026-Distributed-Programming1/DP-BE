ALTER TABLE inquiries
    ADD COLUMN customer_id BIGINT NULL AFTER id;

ALTER TABLE inquiries
    ADD CONSTRAINT fk_inquiries_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id);

CREATE INDEX idx_inquiries_customer_id ON inquiries(customer_id);
