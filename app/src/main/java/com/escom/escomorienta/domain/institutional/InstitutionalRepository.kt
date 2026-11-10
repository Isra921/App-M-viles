package com.escom.escomorienta.domain.institutional

interface InstitutionalRepository {
    suspend fun getAll(): List<InstitutionalResource>
    suspend fun getById(id: String): InstitutionalResource?
    suspend fun getByCategory(category: String): List<InstitutionalResource>
    suspend fun search(query: String): List<InstitutionalResource>
}
