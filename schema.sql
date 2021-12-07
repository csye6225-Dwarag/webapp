
-- Table: public.user_data

-- DROP TABLE public.user_data;


CREATE TABLE IF NOT EXISTS users.users
(
        id binary(255) NOT NULL PRIMARY KEY,
        account_created varchar(50) DEFAULT NULL,
        account_updated varchar(50) DEFAULT NULL,
        verified varchar(50) DEFAULT NULL,
        verified_on varchar(50) DEFAULT NULL,
        email_id varchar(255) NOT NULL,
        first_name varchar(255) NOT NULL,
        last_name varchar(255) NOT NULL,
        password varchar(255) NOT NULL,
        active  bit(1) NOT NULL,
        roles varchar(255) DEFAULT NULL,
        version_num int DEFAULT NULL
) ENGINE=INNODB;;

--     TABLESPACE pg_default;

ALTER TABLE users.users
    OWNER to admin;


-- Table: public.image_data

-- DROP TABLE public.image_data;

-- CREATE TABLE IF NOT EXISTS public.image_data
-- (
--     id character varying(255) COLLATE pg_catalog."default" NOT NULL,
--     file_name character varying(255) COLLATE pg_catalog."default",
--     url character varying(255) COLLATE pg_catalog."default",
--     upload_date character varying(255) COLLATE pg_catalog."default",
--     userid character varying(255) COLLATE pg_catalog."default",
--     CONSTRAINT image_data_pkey PRIMARY KEY (id)
--     )

CREATE TABLE IF NOT EXISTS users.userPic (
    id binary(255) NOT NULL PRIMARY KEY,
    upload_date varchar(50) DEFAULT NULL,
    user_id varchar(255) NOT NULL,
    file_name varchar(255) NOT NULL,
    url varchar(255) NOT NULL
);

--     TABLESPACE pg_default;

ALTER TABLE users.userPic
    OWNER to admin;
