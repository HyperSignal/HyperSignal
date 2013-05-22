USE "/";
CREATE NAMESPACE "hypersignal";
CREATE TABLE datamatrix ( operator, x, y, signal);

LOAD DATA INFILE ROW_KEY_COLUMN="operator"+"x"+"y" HEADER_FILE = "headerfile" "datamatrix.csv.gz" INTO TABLE datamatrix;
