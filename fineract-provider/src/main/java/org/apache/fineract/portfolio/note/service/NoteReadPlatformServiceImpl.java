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
package org.apache.fineract.portfolio.note.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.apache.fineract.portfolio.note.exception.NoteNotFoundException;
import org.apache.fineract.portfolio.note.repository.NoteQuerydslReadRepository;

@RequiredArgsConstructor
public class NoteReadPlatformServiceImpl implements NoteReadPlatformService {

    private final NoteQuerydslReadRepository noteQuerydslReadRepository;

    @Override
    public NoteData retrieveNote(final Long noteId, final Long resourceId, final Integer noteTypeId) {
        Objects.requireNonNull(noteId, "noteId must not be null");
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        final NoteType noteType = validateAndResolveNoteType(noteTypeId);
        return noteQuerydslReadRepository.findNote(noteId, resourceId, noteType)
                .orElseThrow(() -> new NoteNotFoundException(noteId, resourceId, noteType.name().toLowerCase()));
    }

    @Override
    public List<NoteData> retrieveNotesByResource(final Long resourceId, final Integer noteTypeId) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        final NoteType noteType = validateAndResolveNoteType(noteTypeId);
        return noteQuerydslReadRepository.findNotesByResource(resourceId, noteType);
    }

    private NoteType validateAndResolveNoteType(final Integer noteTypeId) {
        Objects.requireNonNull(noteTypeId, "noteTypeId must not be null");
        final NoteType noteType = NoteType.fromInt(noteTypeId);
        if (noteType == null) {
            throw new IllegalArgumentException("Unsupported noteTypeId: " + noteTypeId);
        }
        return noteType;
    }
}
