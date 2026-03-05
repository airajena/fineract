/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.note.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteQuerydslReadRepository {

    private static final QNoteJdbcEntity NOTE = new QNoteJdbcEntity("n");
    private static final QAppUserJdbcEntity CREATED_BY = new QAppUserJdbcEntity("cb");
    private static final QAppUserJdbcEntity MODIFIED_BY = new QAppUserJdbcEntity("mb");

    private final NoteJdbcRepository noteJdbcRepository;

    public Optional<NoteData> findNote(final Long noteId, final Long resourceId, final NoteType noteType) {
        final BooleanExpression predicate = NOTE.id.eq(Objects.requireNonNull(noteId, "noteId must not be null"))
                .and(resourcePredicate(Objects.requireNonNull(noteType, "noteType must not be null"),
                        Objects.requireNonNull(resourceId, "resourceId must not be null")));
        final Tuple result = noteJdbcRepository.query(q -> q
                .select(NOTE.id, NOTE.clientId, NOTE.groupId, NOTE.loanId, NOTE.loanTransactionId, NOTE.noteTypeEnum, NOTE.note,
                        NOTE.createdBy, CREATED_BY.username, NOTE.createdOnUtc, NOTE.createdDate, NOTE.lastModifiedBy, MODIFIED_BY.username,
                        NOTE.lastModifiedOnUtc, NOTE.lastModifiedDate)
                .from(NOTE).leftJoin(CREATED_BY).on(CREATED_BY.id.eq(NOTE.createdBy)).leftJoin(MODIFIED_BY)
                .on(MODIFIED_BY.id.eq(NOTE.lastModifiedBy)).where(predicate).orderBy(NOTE.createdDate.desc()).fetchFirst());
        return Optional.ofNullable(result).map(this::toNoteData);
    }

    public List<NoteData> findNotesByResource(final Long resourceId, final NoteType noteType) {
        return noteJdbcRepository.query(q -> q
                .select(NOTE.id, NOTE.clientId, NOTE.groupId, NOTE.loanId, NOTE.loanTransactionId, NOTE.noteTypeEnum, NOTE.note,
                        NOTE.createdBy, CREATED_BY.username, NOTE.createdOnUtc, NOTE.createdDate, NOTE.lastModifiedBy, MODIFIED_BY.username,
                        NOTE.lastModifiedOnUtc, NOTE.lastModifiedDate)
                .from(NOTE).leftJoin(CREATED_BY).on(CREATED_BY.id.eq(NOTE.createdBy)).leftJoin(MODIFIED_BY)
                .on(MODIFIED_BY.id.eq(NOTE.lastModifiedBy))
                .where(resourcePredicate(Objects.requireNonNull(noteType, "noteType must not be null"),
                        Objects.requireNonNull(resourceId, "resourceId must not be null")))
                .orderBy(NOTE.createdDate.desc()).fetch()).stream().map(this::toNoteData).toList();
    }

    private NoteData toNoteData(final Tuple row) {
        final OffsetDateTime createdOn = resolveOffsetDateTime(row.get(NOTE.createdOnUtc), row.get(NOTE.createdDate));
        final OffsetDateTime updatedOn = resolveOffsetDateTime(row.get(NOTE.lastModifiedOnUtc), row.get(NOTE.lastModifiedDate));
        final Integer noteTypeId = row.get(NOTE.noteTypeEnum);
        return NoteData.builder().id(row.get(NOTE.id)).clientId(row.get(NOTE.clientId)).groupId(row.get(NOTE.groupId))
                .loanId(row.get(NOTE.loanId)).loanTransactionId(row.get(NOTE.loanTransactionId))
                .noteType(NoteType.toEnumOptionData(noteTypeId)).note(row.get(NOTE.note)).createdById(row.get(NOTE.createdBy))
                .createdByUsername(row.get(CREATED_BY.username)).createdOn(createdOn).updatedById(row.get(NOTE.lastModifiedBy))
                .updatedByUsername(row.get(MODIFIED_BY.username)).updatedOn(updatedOn).build();
    }

    private BooleanExpression resourcePredicate(final NoteType noteType, final Long resourceId) {
        return switch (noteType) {
            case CLIENT -> NOTE.clientId.eq(resourceId).and(NOTE.noteTypeEnum.eq(NoteType.CLIENT.getValue()));
            case LOAN -> NOTE.loanId.eq(resourceId)
                    .and(NOTE.noteTypeEnum.eq(NoteType.LOAN.getValue()).or(NOTE.noteTypeEnum.eq(NoteType.LOAN_TRANSACTION.getValue())));
            case LOAN_TRANSACTION -> NOTE.loanTransactionId.eq(resourceId);
            case SAVING_ACCOUNT -> NOTE.savingsAccountId.eq(resourceId).and(NOTE.noteTypeEnum.eq(NoteType.SAVING_ACCOUNT.getValue())
                    .or(NOTE.noteTypeEnum.eq(NoteType.SAVINGS_TRANSACTION.getValue())));
            case SAVINGS_TRANSACTION -> NOTE.savingsAccountTransactionId.eq(resourceId);
            case GROUP -> NOTE.groupId.eq(resourceId);
            default -> throw new IllegalArgumentException("Unsupported note type for resource lookup: " + noteType);
        };
    }

    private OffsetDateTime resolveOffsetDateTime(final Timestamp preferred, final Timestamp fallback) {
        final OffsetDateTime p = toOffsetDateTime(preferred);
        return p != null ? p : toOffsetDateTime(fallback);
    }

    private OffsetDateTime toOffsetDateTime(final Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        final OffsetDateTime atSystem = OffsetDateTime.of(timestamp.toLocalDateTime(),
                DateUtils.getSystemZoneId().getRules().getOffset(timestamp.toLocalDateTime()));
        return atSystem.withOffsetSameInstant(DateUtils.getDateTimeZoneOfTenant().getRules().getOffset(atSystem.toInstant()));
    }
}
