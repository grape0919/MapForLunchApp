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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    // v2: 기록에서 매장 상세로 돌아갈 수 있도록 위치·주소 보존
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = "",
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

    @Query("DELETE FROM excluded")
    suspend fun clearExcluded()

    @Query("SELECT * FROM visits WHERE visitId = :visitId")
    suspend fun visit(visitId: Long): VisitEntity?
}

@Database(
    entities = [FavoriteEntity::class, VisitEntity::class, ExcludedEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gomDao(): GomDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN lat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE visits ADD COLUMN lng REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE visits ADD COLUMN address TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gom-v2.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
