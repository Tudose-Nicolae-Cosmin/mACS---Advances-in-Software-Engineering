<template>
  <section class="analytics">
    <p v-if="loading" class="card">Se incarca statisticile...</p>
    <p v-else-if="error" class="card error">{{ error }}</p>

    <template v-else-if="data">
      <div class="stat-row">
        <div class="card stat">
          <span class="label">Clasament engagement</span>
          <strong v-if="data.rank.topPercent !== null">Top {{ data.rank.topPercent }}%</strong>
          <strong v-else>-</strong>
          <small v-if="data.rank.rank !== null">
            locul {{ data.rank.rank }} din {{ data.rank.totalUsers }} useri
            (medie {{ data.rank.avgEngagement.toFixed(1) }} like-uri + comentarii / postare)
          </small>
          <small v-else>Nu ai inca nicio postare.</small>
        </div>

        <div class="card stat">
          <span class="label">Like-uri / comentarii</span>
          <strong>{{ data.ratio.ratio === null ? '-' : data.ratio.ratio.toFixed(2) }}</strong>
          <small>{{ data.ratio.likes }} like-uri, {{ data.ratio.comments }} comentarii</small>
        </div>
      </div>

      <div class="card">
        <h3>Media like-urilor pe postare</h3>
        <div v-for="row in mediaRows" :key="row.name" class="bar-line">
          <span class="bar-name">{{ row.name }} ({{ row.posts }})</span>
          <div class="bar-track">
            <div class="bar" :style="{ width: row.percent + '%' }"></div>
          </div>
          <span class="bar-value">{{ row.avg === null ? '-' : row.avg.toFixed(1) }}</span>
        </div>
      </div>

      <div class="card">
        <h3>Pe saptamani</h3>
        <p v-if="data.weeks.length === 0">Nicio activitate inca.</p>
        <table v-else>
          <thead>
            <tr><th>Saptamana (de luni)</th><th>Postari</th><th>Like-uri</th><th>Comentarii</th></tr>
          </thead>
          <tbody>
            <tr v-for="w in data.weeks" :key="w.weekStart">
              <td>{{ w.weekStart }}</td><td>{{ w.posts }}</td><td>{{ w.likes }}</td><td>{{ w.comments }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>Top 5 postari</h3>
        <p v-if="data.topPosts.length === 0">Nicio postare inca.</p>
        <ol v-else class="top">
          <li v-for="p in data.topPosts" :key="p.id">
            <span class="content-text">{{ p.content }}</span>
            <span class="counts">{{ p.likes }} like-uri &middot; {{ p.comments }} comentarii &middot; total {{ p.likes + p.comments }}</span>
          </li>
        </ol>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchAnalytics } from './api'

const data = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    data.value = await fetchAnalytics()
  } catch (e) {
    error.value = 'Nu am putut incarca statisticile.'
  } finally {
    loading.value = false
  }
})

const mediaRows = computed(() => {
  const a = data.value.avgLikes
  const max = Math.max(a.photoAvg ?? 0, a.videoAvg ?? 0, 1)
  return [
    { name: 'Poze', avg: a.photoAvg, posts: a.photoPosts, percent: ((a.photoAvg ?? 0) / max) * 100 },
    { name: 'Video', avg: a.videoAvg, posts: a.videoPosts, percent: ((a.videoAvg ?? 0) / max) * 100 }
  ]
})
</script>

<style scoped>
.analytics {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.1);
}

.error {
  color: #b91c1c;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.5rem;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stat .label {
  color: #6b7280;
  font-size: 0.85rem;
}

.stat strong {
  font-size: 2rem;
}

.stat small {
  color: #6b7280;
}

h3 {
  margin: 0 0 1rem;
}

.bar-line {
  display: grid;
  grid-template-columns: 110px 1fr 50px;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.bar-track {
  background: #e5e7eb;
  border-radius: 999px;
  height: 14px;
  overflow: hidden;
}

.bar {
  background: #111827;
  height: 100%;
}

.bar-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.5rem 0.25rem;
  border-bottom: 1px solid #e5e7eb;
}

.top {
  margin: 0;
  padding-left: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.top li .content-text {
  display: block;
}

.counts {
  color: #6b7280;
  font-size: 0.85rem;
}
</style>