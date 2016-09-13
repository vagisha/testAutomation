-- For testing merge, need to have the PK on this table not be IDENTITY
DROP TABLE vehicle.etl_target;
CREATE TABLE vehicle.etl_target(
  RowId INT NOT NULL,
  container entityid,
  created DATETIME,
  modified DATETIME,
  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);


CREATE TABLE vehicle.etl_180column_source(
  RowId int IDENTITY(1,1) NOT NULL,
  container dbo.ENTITYID NULL,
  created datetime NULL,
  modified datetime NULL,
  field5 INT NULL,
  field6 INT NULL,
  field7 INT NULL,
  field8 INT NULL,
  field9 INT NULL,
  field10 INT NULL,
  field11 INT NULL,
  field12 INT NULL,
  field13 INT NULL,
  field14 INT NULL,
  field15 INT NULL,
  field16 INT NULL,
  field17 INT NULL,
  field18 INT NULL,
  field19 INT NULL,
  field20 INT NULL,
  field21 INT NULL,
  field22 INT NULL,
  field23 INT NULL,
  field24 INT NULL,
  field25 INT NULL,
  field26 INT NULL,
  field27 INT NULL,
  field28 INT NULL,
  field29 INT NULL,
  field30 INT NULL,
  field31 INT NULL,
  field32 INT NULL,
  field33 INT NULL,
  field34 INT NULL,
  field35 INT NULL,
  field36 INT NULL,
  field37 INT NULL,
  field38 INT NULL,
  field39 INT NULL,
  field40 INT NULL,
  field41 INT NULL,
  field42 INT NULL,
  field43 INT NULL,
  field44 INT NULL,
  field45 INT NULL,
  field46 INT NULL,
  field47 INT NULL,
  field48 INT NULL,
  field49 INT NULL,
  field50 INT NULL,
  field51 INT NULL,
  field52 INT NULL,
  field53 INT NULL,
  field54 INT NULL,
  field55 INT NULL,
  field56 INT NULL,
  field57 INT NULL,
  field58 INT NULL,
  field59 INT NULL,
  field60 INT NULL,
  field61 INT NULL,
  field62 INT NULL,
  field63 INT NULL,
  field64 INT NULL,
  field65 INT NULL,
  field66 INT NULL,
  field67 INT NULL,
  field68 INT NULL,
  field69 INT NULL,
  field70 INT NULL,
  field71 INT NULL,
  field72 INT NULL,
  field73 INT NULL,
  field74 INT NULL,
  field75 INT NULL,
  field76 INT NULL,
  field77 INT NULL,
  field78 INT NULL,
  field79 INT NULL,
  field80 INT NULL,
  field81 INT NULL,
  field82 INT NULL,
  field83 INT NULL,
  field84 INT NULL,
  field85 INT NULL,
  field86 INT NULL,
  field87 INT NULL,
  field88 INT NULL,
  field89 INT NULL,
  field90 INT NULL,
  field91 INT NULL,
  field92 INT NULL,
  field93 INT NULL,
  field94 INT NULL,
  field95 INT NULL,
  field96 INT NULL,
  field97 INT NULL,
  field98 INT NULL,
  field99 INT NULL,
  field100 INT NULL,
  field101 INT NULL,
  field102 INT NULL,
  field103 INT NULL,
  field104 INT NULL,
  field105 INT NULL,
  field106 INT NULL,
  field107 INT NULL,
  field108 INT NULL,
  field109 INT NULL,
  field110 INT NULL,
  field111 INT NULL,
  field112 INT NULL,
  field113 INT NULL,
  field114 INT NULL,
  field115 INT NULL,
  field116 INT NULL,
  field117 INT NULL,
  field118 INT NULL,
  field119 INT NULL,
  field120 INT NULL,
  field121 INT NULL,
  field122 INT NULL,
  field123 INT NULL,
  field124 INT NULL,
  field125 INT NULL,
  field126 INT NULL,
  field127 INT NULL,
  field128 INT NULL,
  field129 INT NULL,
  field130 INT NULL,
  field131 INT NULL,
  field132 INT NULL,
  field133 INT NULL,
  field134 INT NULL,
  field135 INT NULL,
  field136 INT NULL,
  field137 INT NULL,
  field138 INT NULL,
  field139 INT NULL,
  field140 INT NULL,
  field141 INT NULL,
  field142 INT NULL,
  field143 INT NULL,
  field144 INT NULL,
  field145 INT NULL,
  field146 INT NULL,
  field147 INT NULL,
  field148 INT NULL,
  field149 INT NULL,
  field150 INT NULL,
  field151 INT NULL,
  field152 INT NULL,
  field153 INT NULL,
  field154 INT NULL,
  field155 INT NULL,
  field156 INT NULL,
  field157 INT NULL,
  field158 INT NULL,
  field159 INT NULL,
  field160 INT NULL,
  field161 INT NULL,
  field162 INT NULL,
  field163 INT NULL,
  field164 INT NULL,
  field165 INT NULL,
  field166 INT NULL,
  field167 INT NULL,
  field168 INT NULL,
  field169 INT NULL,
  field170 INT NULL,
  field171 INT NULL,
  field172 INT NULL,
  field173 INT NULL,
  field174 INT NULL,
  field175 INT NULL,
  field176 INT NULL,
  field177 INT NULL,
  field178 INT NULL,
  field179 INT NULL,
  field180 INT NULL,

  CONSTRAINT PK_etl_180column_source PRIMARY KEY (RowId),
  CONSTRAINT FK_etl_180column_source_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

CREATE TABLE vehicle.etl_180column_target(
  RowId int NOT NULL,
  container dbo.ENTITYID NULL,
  created datetime NULL,
  modified datetime NULL,
  field5 INT NULL,
  field6 INT NULL,
  field7 INT NULL,
  field8 INT NULL,
  field9 INT NULL,
  field10 INT NULL,
  field11 INT NULL,
  field12 INT NULL,
  field13 INT NULL,
  field14 INT NULL,
  field15 INT NULL,
  field16 INT NULL,
  field17 INT NULL,
  field18 INT NULL,
  field19 INT NULL,
  field20 INT NULL,
  field21 INT NULL,
  field22 INT NULL,
  field23 INT NULL,
  field24 INT NULL,
  field25 INT NULL,
  field26 INT NULL,
  field27 INT NULL,
  field28 INT NULL,
  field29 INT NULL,
  field30 INT NULL,
  field31 INT NULL,
  field32 INT NULL,
  field33 INT NULL,
  field34 INT NULL,
  field35 INT NULL,
  field36 INT NULL,
  field37 INT NULL,
  field38 INT NULL,
  field39 INT NULL,
  field40 INT NULL,
  field41 INT NULL,
  field42 INT NULL,
  field43 INT NULL,
  field44 INT NULL,
  field45 INT NULL,
  field46 INT NULL,
  field47 INT NULL,
  field48 INT NULL,
  field49 INT NULL,
  field50 INT NULL,
  field51 INT NULL,
  field52 INT NULL,
  field53 INT NULL,
  field54 INT NULL,
  field55 INT NULL,
  field56 INT NULL,
  field57 INT NULL,
  field58 INT NULL,
  field59 INT NULL,
  field60 INT NULL,
  field61 INT NULL,
  field62 INT NULL,
  field63 INT NULL,
  field64 INT NULL,
  field65 INT NULL,
  field66 INT NULL,
  field67 INT NULL,
  field68 INT NULL,
  field69 INT NULL,
  field70 INT NULL,
  field71 INT NULL,
  field72 INT NULL,
  field73 INT NULL,
  field74 INT NULL,
  field75 INT NULL,
  field76 INT NULL,
  field77 INT NULL,
  field78 INT NULL,
  field79 INT NULL,
  field80 INT NULL,
  field81 INT NULL,
  field82 INT NULL,
  field83 INT NULL,
  field84 INT NULL,
  field85 INT NULL,
  field86 INT NULL,
  field87 INT NULL,
  field88 INT NULL,
  field89 INT NULL,
  field90 INT NULL,
  field91 INT NULL,
  field92 INT NULL,
  field93 INT NULL,
  field94 INT NULL,
  field95 INT NULL,
  field96 INT NULL,
  field97 INT NULL,
  field98 INT NULL,
  field99 INT NULL,
  field100 INT NULL,
  field101 INT NULL,
  field102 INT NULL,
  field103 INT NULL,
  field104 INT NULL,
  field105 INT NULL,
  field106 INT NULL,
  field107 INT NULL,
  field108 INT NULL,
  field109 INT NULL,
  field110 INT NULL,
  field111 INT NULL,
  field112 INT NULL,
  field113 INT NULL,
  field114 INT NULL,
  field115 INT NULL,
  field116 INT NULL,
  field117 INT NULL,
  field118 INT NULL,
  field119 INT NULL,
  field120 INT NULL,
  field121 INT NULL,
  field122 INT NULL,
  field123 INT NULL,
  field124 INT NULL,
  field125 INT NULL,
  field126 INT NULL,
  field127 INT NULL,
  field128 INT NULL,
  field129 INT NULL,
  field130 INT NULL,
  field131 INT NULL,
  field132 INT NULL,
  field133 INT NULL,
  field134 INT NULL,
  field135 INT NULL,
  field136 INT NULL,
  field137 INT NULL,
  field138 INT NULL,
  field139 INT NULL,
  field140 INT NULL,
  field141 INT NULL,
  field142 INT NULL,
  field143 INT NULL,
  field144 INT NULL,
  field145 INT NULL,
  field146 INT NULL,
  field147 INT NULL,
  field148 INT NULL,
  field149 INT NULL,
  field150 INT NULL,
  field151 INT NULL,
  field152 INT NULL,
  field153 INT NULL,
  field154 INT NULL,
  field155 INT NULL,
  field156 INT NULL,
  field157 INT NULL,
  field158 INT NULL,
  field159 INT NULL,
  field160 INT NULL,
  field161 INT NULL,
  field162 INT NULL,
  field163 INT NULL,
  field164 INT NULL,
  field165 INT NULL,
  field166 INT NULL,
  field167 INT NULL,
  field168 INT NULL,
  field169 INT NULL,
  field170 INT NULL,
  field171 INT NULL,
  field172 INT NULL,
  field173 INT NULL,
  field174 INT NULL,
  field175 INT NULL,
  field176 INT NULL,
  field177 INT NULL,
  field178 INT NULL,
  field179 INT NULL,
  field180 INT NULL,

  CONSTRAINT PK_etl_180column_target PRIMARY KEY (RowId),
  CONSTRAINT FK_etl_180column_target_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);
