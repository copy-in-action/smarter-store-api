-- Venue Table
CREATE TABLE venue (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    seating_chart_url VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Performance Table
CREATE TABLE performance (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    running_time INT,
    age_rating VARCHAR(50),
    main_image_url VARCHAR(255),
    venue_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (venue_id) REFERENCES venue (id)
);

-- Performance Schedule Table
CREATE TABLE performance_schedule (
    id BIGSERIAL PRIMARY KEY,
    performance_id BIGINT NOT NULL,
    show_datetime TIMESTAMP NOT NULL,
    sale_start_datetime TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE CASCADE
);

-- Ticket Option Table
CREATE TABLE ticket_option (
    id BIGSERIAL PRIMARY KEY,
    performance_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_quantity INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (performance_id) REFERENCES performance (id) ON DELETE CASCADE
);
