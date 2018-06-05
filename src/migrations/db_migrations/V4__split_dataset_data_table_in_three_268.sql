ALTER TABLE dataset_data RENAME dataset_data_positions;

CREATE TABLE dataset_data_water_at_intake(
id int NOT NULL auto_increment PRIMARY KEY,
dataset_data_positions_id INT NOT NULL,
intake_temperature DOUBLE NULL,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
modified DATETIME,
CONSTRAINT FOREIGN KEY in_dataset_data_positions_ix(dataset_data_positions_id)
REFERENCES dataset_data_positions(id) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE TRIGGER `intake_insert_trigger`
BEFORE INSERT ON  `dataset_data_water_at_intake`
FOR EACH ROW SET NEW.modified=NOW();

CREATE TRIGGER `intake_update_trigger`
BEFORE UPDATE ON  `dataset_data_water_at_intake`
FOR EACH ROW SET NEW.modified=NOW();

INSERT INTO dataset_data_water_at_intake
(dataset_data_positions_id,intake_temperature, created)
SELECT id, intake_temperature, created from dataset_data_positions;

CREATE TABLE dataset_data_water_at_equilibrator(
id int NOT NULL auto_increment PRIMARY KEY,
dataset_data_positions_id INT NOT NULL,
shifted_dataset_data_positions_id INT NOT NULL,
run_type varchar(45) NOT NULL,
diagnostic_values TEXT NULL,
salinity DOUBLE NULL,
equilibrator_temperature DOUBLE NULL,
equilibrator_pressure_absolute DOUBLE NULL,
equilibrator_pressure_differential DOUBLE NULL,
atmospheric_pressure DOUBLE NULL,
xh2o DOUBLE NULL,
co2 DOUBLE NULL,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
modified DATETIME,
CONSTRAINT FOREIGN KEY eq_dataset_data_positions_ix(dataset_data_positions_id)
REFERENCES dataset_data_positions(id) ON UPDATE RESTRICT ON DELETE CASCADE,
CONSTRAINT FOREIGN KEY
shifted_dataset_data_positions_ix(shifted_dataset_data_positions_id)
REFERENCES dataset_data_positions(id) ON UPDATE RESTRICT ON DELETE CASCADE
);

CREATE TRIGGER `equilibrator_insert_trigger`
BEFORE INSERT ON  `dataset_data_water_at_equilibrator`
FOR EACH ROW SET NEW.modified=NOW();

CREATE TRIGGER `equilibrator_update_trigger`
BEFORE UPDATE ON  `dataset_data_water_at_equilibrator`
FOR EACH ROW SET NEW.modified=NOW();

INSERT INTO dataset_data_water_at_equilibrator (dataset_data_positions_id,
shifted_dataset_data_positions_id,
run_type, diagnostic_values, salinity, equilibrator_temperature,
equilibrator_pressure_absolute, equilibrator_pressure_differential,
atmospheric_pressure, xh2o, co2, created
)
SELECT id, id, run_type, diagnostic_values, salinity, equilibrator_temperature,
equilibrator_pressure_absolute, equilibrator_pressure_differential,
atmospheric_pressure, xh2o, co2, created from dataset_data_positions;

ALTER TABLE dataset_data_positions DROP run_type, DROP diagnostic_values,
DROP salinity, DROP equilibrator_temperature,
DROP equilibrator_pressure_absolute, DROP equilibrator_pressure_differential,
DROP atmospheric_pressure, DROP xh2o, DROP co2, DROP intake_temperature;


-- Create a view that mimic the functionality of the dataset_data table
CREATE VIEW dataset_data AS SELECT ddp.id, ddp.dataset_id, ddp.date,
ddp.longitude, ddp.latitude, ddq.run_type, ddq.diagnostic_values,
ddi.intake_temperature, ddq.salinity, ddq.equilibrator_temperature,
ddq.equilibrator_pressure_absolute, ddq.equilibrator_pressure_differential,
ddq.atmospheric_pressure, ddq.xh2o, ddq.co2, ddp.created, ddp.modified
FROM dataset_data_positions ddp
INNER JOIN dataset_data_water_at_equilibrator ddq
ON (ddp.id=ddq.shifted_dataset_data_positions_id)
INNER JOIN dataset_data_water_at_intake ddi
ON (ddp.id=ddi.dataset_data_positions_id);


-- Modify instrument table
ALTER TABLE instrument ADD COLUMN time_measurement_delay INT NOT NULL
DEFAULT 0 AFTER platform_code;

-- Analyze tables after the updates
ANALYZE TABLE dataset_data_positions;
ANALYZE TABLE dataset_data_water_at_intake;
ANALYZE TABLE dataset_data_water_at_equilibrator;

