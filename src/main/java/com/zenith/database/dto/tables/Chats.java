/*
 * This file is generated by jOOQ.
 */
package com.zenith.database.dto.tables;


import com.zenith.database.dto.Indexes;
import com.zenith.database.dto.Public;
import com.zenith.database.dto.tables.records.ChatsRecord;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Chats extends TableImpl<ChatsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.chats</code>
     */
    public static final Chats CHATS = new Chats();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ChatsRecord> getRecordType() {
        return ChatsRecord.class;
    }

    /**
     * The column <code>public.chats.time</code>.
     */
    public final TableField<ChatsRecord, OffsetDateTime> TIME = createField(DSL.name("time"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

    /**
     * The column <code>public.chats.chat</code>.
     */
    public final TableField<ChatsRecord, String> CHAT = createField(DSL.name("chat"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.chats.player_name</code>.
     */
    public final TableField<ChatsRecord, String> PLAYER_NAME = createField(DSL.name("player_name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.chats.player_uuid</code>.
     */
    public final TableField<ChatsRecord, UUID> PLAYER_UUID = createField(DSL.name("player_uuid"), SQLDataType.UUID, this, "");

    private Chats(Name alias, Table<ChatsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Chats(Name alias, Table<ChatsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.chats</code> table reference
     */
    public Chats(String alias) {
        this(DSL.name(alias), CHATS);
    }

    /**
     * Create an aliased <code>public.chats</code> table reference
     */
    public Chats(Name alias) {
        this(alias, CHATS);
    }

    /**
     * Create a <code>public.chats</code> table reference
     */
    public Chats() {
        this(DSL.name("chats"), null);
    }

    public <O extends Record> Chats(Table<O> child, ForeignKey<O, ChatsRecord> key) {
        super(child, key, CHATS);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.CHATS_PLAYER_UUID_IDX, Indexes.CHATS_TIME_IDX);
    }

    @Override
    public Chats as(String alias) {
        return new Chats(DSL.name(alias), this);
    }

    @Override
    public Chats as(Name alias) {
        return new Chats(alias, this);
    }

    @Override
    public Chats as(Table<?> alias) {
        return new Chats(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Chats rename(String name) {
        return new Chats(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Chats rename(Name name) {
        return new Chats(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Chats rename(Table<?> name) {
        return new Chats(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<OffsetDateTime, String, String, UUID> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function4<? super OffsetDateTime, ? super String, ? super String, ? super UUID, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function4<? super OffsetDateTime, ? super String, ? super String, ? super UUID, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
