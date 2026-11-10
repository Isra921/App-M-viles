package com.escom.escomorienta.domain.institutional

class GetInstitutionalResourcesUseCase(private val repository: InstitutionalRepository) {
    suspend operator fun invoke() = repository.getAll()
}
class GetInstitutionalResourceByIdUseCase(private val repository: InstitutionalRepository) {
    suspend operator fun invoke(id: String) = repository.getById(id)
}
class GetInstitutionalResourcesByCategoryUseCase(private val repository: InstitutionalRepository) {
    suspend operator fun invoke(category: String) = repository.getByCategory(category)
}
class SearchInstitutionalResourcesUseCase(private val repository: InstitutionalRepository) {
    suspend operator fun invoke(query: String) = repository.search(query)
}
