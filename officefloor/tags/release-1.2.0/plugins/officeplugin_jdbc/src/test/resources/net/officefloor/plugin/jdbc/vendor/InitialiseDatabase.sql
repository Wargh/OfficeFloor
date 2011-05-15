CREATE TABLE PRODUCT
( PRODUCT_ID    INTEGER
, PRODUCT_NAME  VARCHAR(20)
);
ALTER TABLE PRODUCT ADD PRIMARY KEY ( PRODUCT_ID );
CREATE INDEX IDX_PRODUCT_NAME ON PRODUCT ( PRODUCT_NAME );

INSERT INTO PRODUCT ( PRODUCT_ID, PRODUCT_NAME ) VALUES ( 1, 'Test' );
