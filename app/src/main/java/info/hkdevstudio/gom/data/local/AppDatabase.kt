package info.hkdevstudio.gom.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val placeUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val visitId: Long = 0,
    val placeId: String,
    val name: String,
    val category: String,
    val placeUrl: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val rating: Int = 0,      // 0 = 미평가, 1~5
    val note: String = "",    // 나만의 한 줄 기록
)

@Entity(tableName = "excluded")
data class ExcludedEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface GomDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun favorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT * FROM visits ORDER BY visitedAt DESC")
    fun visits(): Flow<List<VisitEntity>>

    @Insert
    suspend fun addVisit(visit: VisitEntity): Long

    @Query("UPDATE visits SET rating = :rating, note = :note WHERE visitId = :visitId")
    suspend fun updateVisit(visitId: Long, rating: Int, note: String)

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)

    @Query("SELECT * FROM excluded ORDER BY createdAt DESC")
    fun excluded(): Flow<List<ExcludedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun exclude(entity: ExcludedEntity)

    @Query("DELETE FROM excluded WHERE id = :id")
    suspend fun include(id: String)
}

@Database(
    entities = [FavoriteEntity::class, VisitEntity::class, ExcludedEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gomDao(): GomDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gom-v2.db",
                ).build().also { instance = it }
            }
    }
}
