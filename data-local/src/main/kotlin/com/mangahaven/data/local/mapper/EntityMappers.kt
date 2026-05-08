package com.mangahaven.data.local.mapper

import com.mangahaven.data.local.entity.LibraryItemEntity
import com.mangahaven.data.local.entity.ReadingProgressEntity
import com.mangahaven.data.local.entity.SourceEntity
import com.mangahaven.model.*

/**
 * Source ↔ SourceEntity 映射。
 */
fun Source.toEntity(): SourceEntity = SourceEntity(
    id = id,
    type = type.name,
    name = name,
    configJson = configJson,
    authRef = authRef,
    isWritable = isWritable,
    lastSyncAt = lastSyncAt,
    isVirtual = isVirtual,
)

fun SourceEntity.toModel(): Source = Source(
    id = id,
    type = SourceType.valueOf(type),
    name = name,
    configJson = configJson,
    authRef = authRef,
    isWritable = isWritable,
    lastSyncAt = lastSyncAt,
    isVirtual = isVirtual,
)

/**
 * LibraryItem ↔ LibraryItemEntity 映射。
 */
fun LibraryItem.toEntity(): LibraryItemEntity = LibraryItemEntity(
    id = id,
    sourceId = sourceId,
    path = path,
    title = title,
    coverPath = coverPath,
    itemType = itemType.name,
    pageCount = pageCount,
    readingStatus = readingStatus.name,
    isFavorite = isFavorite,
    lastReadAt = lastReadAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun LibraryItemEntity.toModel(): LibraryItem = LibraryItem(
    id = id,
    sourceId = sourceId,
    path = path,
    title = title,
    coverPath = coverPath,
    itemType = LibraryItemType.valueOf(itemType),
    pageCount = pageCount,
    readingStatus = ReadingStatus.valueOf(readingStatus),
    isFavorite = isFavorite,
    lastReadAt = lastReadAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * ReadingProgress ↔ ReadingProgressEntity 映射。
 */
fun ReadingProgress.toEntity(): ReadingProgressEntity = ReadingProgressEntity(
    itemId = itemId,
    currentPage = currentPage,
    totalPages = totalPages,
    scrollOffset = scrollOffset,
    readingMode = readingMode.name,
    updatedAt = updatedAt,
)

fun ReadingProgressEntity.toModel(): ReadingProgress = ReadingProgress(
    itemId = itemId,
    currentPage = currentPage,
    totalPages = totalPages,
    scrollOffset = scrollOffset,
    readingMode = ReadingMode.valueOf(readingMode),
    updatedAt = updatedAt,
)
