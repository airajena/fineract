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
package org.apache.fineract.portfolio.note.starter;

import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.SQLTemplatesRegistry;
import com.querydsl.sql.spring.SpringConnectionProvider;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.group.domain.GroupRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.note.repository.NoteQuerydslReadRepository;
import org.apache.fineract.portfolio.note.service.NoteReadPlatformService;
import org.apache.fineract.portfolio.note.service.NoteReadPlatformServiceImpl;
import org.apache.fineract.portfolio.note.service.NoteWritePlatformService;
import org.apache.fineract.portfolio.note.service.NoteWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NoteReadPlatformService noteReadPlatformService(NoteQuerydslReadRepository noteQuerydslReadRepository) {
        return new NoteReadPlatformServiceImpl(noteQuerydslReadRepository);
    }

    @Bean("noteSqlTemplates")
    @ConditionalOnMissingBean(name = "noteSqlTemplates")
    public SQLTemplates noteSqlTemplates(@Qualifier("dataSource") DataSource dataSource) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return new SQLTemplatesRegistry().getTemplates(connection.getMetaData());
        }
    }

    @Bean("noteSqlQueryFactory")
    @ConditionalOnMissingBean(name = "noteSqlQueryFactory")
    public SQLQueryFactory noteSqlQueryFactory(@Qualifier("dataSource") DataSource dataSource,
            @Qualifier("noteSqlTemplates") SQLTemplates noteSqlTemplates) {
        final com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(noteSqlTemplates);
        return new SQLQueryFactory(configuration, new SpringConnectionProvider(dataSource));
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteQuerydslReadRepository noteQuerydslReadRepository(
            @Qualifier("noteSqlQueryFactory") SQLQueryFactory noteSqlQueryFactory) {
        return new NoteQuerydslReadRepository(noteSqlQueryFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public NoteWritePlatformService noteWritePlatformService(NoteRepository noteRepository, ClientRepositoryWrapper clientRepository,
            GroupRepository groupRepository, LoanRepositoryWrapper loanRepository, LoanTransactionRepository loanTransactionRepository,
            SavingsAccountRepository savingsAccountRepository) {
        return new NoteWritePlatformServiceJpaRepositoryImpl(noteRepository, clientRepository, groupRepository, loanRepository,
                loanTransactionRepository, savingsAccountRepository);
    }
}
