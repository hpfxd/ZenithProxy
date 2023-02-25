/*
 * This file is generated by jOOQ.
 */
package com.zenith.database.dto.tables;


import com.zenith.database.dto.Public;
import com.zenith.database.dto.tables.records.ImportDeathRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ImportDeath extends TableImpl<ImportDeathRecord> {

    /**
     * The reference instance of <code>public.import_death</code>
     */
    public static final ImportDeath IMPORT_DEATH = new ImportDeath();
    private static final long serialVersionUID = 1L;
    /**
     * The column <code>public.import_death.time</code>.
     */
    public final TableField<ImportDeathRecord, OffsetDateTime> TIME = createField(DSL.name("time"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");
    /**
     * The column <code>public.import_death.death_message</code>.
     */
    public final TableField<ImportDeathRecord, String> DEATH_MESSAGE = createField(DSL.name("death_message"), SQLDataType.CLOB.nullable(false), this, "");
    /**
     * The column <code>public.import_death.victim_player_name</code>.
     */
    public final TableField<ImportDeathRecord, String> VICTIM_PLAYER_NAME = createField(DSL.name("victim_player_name"), SQLDataType.CLOB.nullable(false), this, "");
    /**
     * The column <code>public.import_death.victim_player_uuid</code>.
     */
    public final TableField<ImportDeathRecord, UUID> VICTIM_PLAYER_UUID = createField(DSL.name("victim_player_uuid"), SQLDataType.UUID.nullable(false), this, "");
    /**
     * The column <code>public.import_death.killer_player_name</code>.
     */
    public final TableField<ImportDeathRecord, String> KILLER_PLAYER_NAME = createField(DSL.name("killer_player_name"), SQLDataType.CLOB, this, "");
    /**
     * The column <code>public.import_death.killer_player_uuid</code>.
     */
    public final TableField<ImportDeathRecord, UUID> KILLER_PLAYER_UUID = createField(DSL.name("killer_player_uuid"), SQLDataType.UUID, this, "");
    /**
     * The column <code>public.import_death.weapon_name</code>.
     */
    public final TableField<ImportDeathRecord, String> WEAPON_NAME = createField(DSL.name("weapon_name"), SQLDataType.CLOB, this, "");
    /**
     * The column <code>public.import_death.killer_mob</code>.
     */
    public final TableField<ImportDeathRecord, String> KILLER_MOB = createField(DSL.name("killer_mob"), SQLDataType.CLOB, this, "");

    private ImportDeath(Name alias, Table<ImportDeathRecord> aliased) {
        this(alias, aliased, null);
    }

    private ImportDeath(Name alias, Table<ImportDeathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.import_death</code> table reference
     */
    public ImportDeath(String alias) {
        this(DSL.name(alias), IMPORT_DEATH);
    }

    /**
     * Create an aliased <code>public.import_death</code> table reference
     */
    public ImportDeath(Name alias) {
        this(alias, IMPORT_DEATH);
    }

    /**
     * Create a <code>public.import_death</code> table reference
     */
    public ImportDeath() {
        this(DSL.name("import_death"), null);
    }

    public <O extends Record> ImportDeath(Table<O> child, ForeignKey<O, ImportDeathRecord> key) {
        super(child, key, IMPORT_DEATH);
    }

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ImportDeathRecord> getRecordType() {
        return ImportDeathRecord.class;
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public ImportDeath as(String alias) {
        return new ImportDeath(DSL.name(alias), this);
    }

    @Override
    public ImportDeath as(Name alias) {
        return new ImportDeath(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ImportDeath rename(String name) {
        return new ImportDeath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ImportDeath rename(Name name) {
        return new ImportDeath(name, null);
    }

    // -------------------------------------------------------------------------
    // Row8 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row8<OffsetDateTime, String, String, UUID, String, UUID, String, String> fieldsRow() {
        return (Row8) super.fieldsRow();
    }
}