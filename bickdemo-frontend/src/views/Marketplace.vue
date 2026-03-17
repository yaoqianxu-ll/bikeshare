<template>
  <div class="marketplace-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-head">
        <div>
          <div class="hero-kicker">BikeShare Marketplace</div>
          <h1>附近可租、我要出租、咨询车主、申请时间线</h1>
          <p>平台车继续直接租，个人车走咨询、申请和线下交付时间线。</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="handleLocateNearby" :loading="locating">获取附近可租</el-button>
          <el-button plain v-if="userStore.isLoggedIn" @click="openListingDialog()">我要出租</el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="附近可租" name="discover">
        <el-card shadow="never">
          <div class="toolbar">
            <el-space wrap>
              <el-select v-model="discoverFilters.type" clearable placeholder="车型筛选" style="width: 160px">
                <el-option label="山地车" value="MOUNTAIN" />
                <el-option label="公路车" value="ROAD" />
                <el-option label="城市车" value="CITY" />
                <el-option label="电动车" value="ELECTRIC" />
                <el-option label="双人车" value="TANDEM" />
              </el-select>
              <el-select v-model="discoverFilters.radiusKm" style="width: 140px">
                <el-option :value="3" label="3 公里" />
                <el-option :value="5" label="5 公里" />
                <el-option :value="8" label="8 公里" />
                <el-option :value="15" label="15 公里" />
              </el-select>
              <el-button plain @click="loadDiscover" :loading="discoverLoading">刷新推荐</el-button>
              <el-button plain @click="handleLocateNearby" :loading="locating">使用我的位置</el-button>
            </el-space>
            <span class="toolbar-text">{{ currentLocationText }}</span>
          </div>

          <el-empty v-if="!discoverLoading && !discoverItems.length" description="附近还没有合适资源" :image-size="88" />

          <el-row v-else v-loading="discoverLoading" :gutter="16">
            <el-col v-for="item in discoverItems" :key="`${item.sourceType}-${item.sourceId}`" :xs="24" :sm="12" :lg="8" class="card-col">
              <el-card shadow="hover" class="resource-card">
                <template #header>
                  <div class="card-header">
                    <div>
                      <strong>{{ item.title }}</strong>
                      <div class="muted">{{ getTypeLabel(item.type) }} · {{ item.location || '未填写地点' }}</div>
                    </div>
                    <el-space wrap>
                      <el-tag :type="getDiscoverSourceTagType(item)">{{ getDiscoverSourceTagLabel(item) }}</el-tag>
                      <el-tag v-if="item.distanceKm !== null && item.distanceKm !== undefined" type="warning" effect="plain">{{ formatDistance(item.distanceKm) }}</el-tag>
                    </el-space>
                  </div>
                </template>
                <el-image v-if="item.imageUrl" :src="item.imageUrl" fit="cover" class="cover" />
                <div class="price-line">{{ formatMoney(item.pricePerHour) }} / 小时</div>
                <p class="muted">{{ item.description || '暂无更多说明。' }}</p>
                <div class="meta-list">
                  <div v-if="item.sourceType === 'OWNER'">车主：{{ item.ownerUsername }}</div>
                  <div v-if="item.sourceType === 'OWNER'">交付：{{ getDeliveryModeLabel(item.deliveryMode) }}</div>
                  <div v-if="item.sourceType === 'PLATFORM'">库存：{{ item.quantity ?? 1 }}</div>
                </div>
                <el-space wrap>
                  <el-button v-if="item.sourceType === 'PLATFORM'" type="primary" @click="goToPlatformRentals">前往平台租用</el-button>
                  <template v-else-if="!isOwnListing(item)">
                    <el-button plain @click="consultOwner(item)">咨询车主</el-button>
                    <el-button type="primary" @click="openApplicationDialog(item)">申请租用</el-button>
                  </template>
                  <el-tag v-else type="info" effect="plain">这是你发布的车辆</el-tag>
                </el-space>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我要出租" name="listings">
        <el-card shadow="never">
          <el-empty v-if="!userStore.isLoggedIn" description="登录后可以发布自己的出租车辆">
            <el-button type="primary" @click="goLogin">去登录</el-button>
          </el-empty>
          <template v-else>
            <div class="toolbar">
              <div>
                <strong>我的挂牌</strong>
                <div class="muted">上传车辆后会先进入管理员审核，审核通过后才会展示给租客。</div>
              </div>
              <el-button type="primary" @click="openListingDialog()">发布出租车辆</el-button>
            </div>
            <el-empty v-if="!marketLoading && !myListings.length" description="还没有个人出租挂牌" />
            <el-row v-else :gutter="16" v-loading="marketLoading">
              <el-col v-for="listing in myListings" :key="listing.id" :xs="24" :sm="12" :lg="8" class="card-col">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <div>
                        <strong>{{ listing.name }}</strong>
                        <div class="muted">{{ listing.location }}</div>
                      </div>
                      <el-space wrap>
                        <el-tag :type="getReviewStatusType(listing.reviewStatus)">{{ getReviewStatusLabel(listing.reviewStatus) }}</el-tag>
                        <el-tag v-if="shouldShowListingStatus(listing)" :type="getListingStatusType(listing.status)" effect="plain">{{ getListingStatusLabel(listing.status) }}</el-tag>
                      </el-space>
                    </div>
                  </template>
                  <div class="meta-list">
                    <div>{{ formatMoney(listing.pricePerHour) }}/小时</div>
                    <div>{{ getDeliveryModeLabel(listing.deliveryMode) }}</div>
                    <div>进行中申请 {{ listing.activeApplicationCount ?? 0 }}</div>
                  </div>
                  <div class="review-hint" :class="`review-hint--${(listing.reviewStatus || '').toLowerCase()}`">
                    {{ getReviewStatusHint(listing) }}
                  </div>
                  <div v-if="listing.reviewRemark" class="review-remark">审核备注：{{ listing.reviewRemark }}</div>
                  <p class="muted">{{ listing.description || '暂无车辆说明。' }}</p>
                  <el-space wrap>
                    <el-button plain @click="openListingDialog(listing)">编辑</el-button>
                    <el-button
                      v-if="listing.reviewStatus === 'APPROVED'"
                      plain
                      :type="listing.status === 'OFFLINE' ? 'success' : 'warning'"
                      @click="toggleListingStatus(listing)"
                    >
                      {{ listing.status === 'OFFLINE' ? '重新上架' : '下架' }}
                    </el-button>
                  </el-space>
                </el-card>
              </el-col>
            </el-row>

            <div class="sub-title">车主收到的申请</div>
            <el-empty v-if="!marketLoading && !ownerApplications.length" description="暂时还没有申请" />
            <div v-else class="stack" v-loading="marketLoading">
              <el-card v-for="app in ownerApplications" :key="app.id" shadow="never">
                <div class="card-header">
                  <div>
                    <strong>{{ app.listingTitle }}</strong>
                    <div class="muted">申请人：{{ app.renterUsername }} · {{ formatDateRange(app.requestedStartTime, app.requestedEndTime) }}</div>
                  </div>
                  <el-space wrap>
                    <el-tag :type="getApplicationStatusType(app.status)">{{ getApplicationStatusLabel(app.status) }}</el-tag>
                    <el-button text @click="consultPeer(app.renterId, app.listingTitle)">发消息</el-button>
                  </el-space>
                </div>
                <p class="muted">{{ app.renterMessage || '租客还没有留下说明。' }}</p>
                <div class="meta-list">
                  <div>交付地点：{{ app.meetupLocation || '待确认' }}</div>
                  <div v-if="app.meetupTime">交付时间：{{ formatDateTime(app.meetupTime) }}</div>
                </div>
                <el-space wrap>
                  <el-button v-if="app.status === 'PENDING_OWNER_CONFIRMATION'" plain @click="updateOwnerApplication(app, 'NEGOTIATING')">沟通中</el-button>
                  <el-button v-if="['PENDING_OWNER_CONFIRMATION', 'NEGOTIATING'].includes(app.status)" type="primary" @click="updateOwnerApplication(app, 'CONFIRMED')">确认出租</el-button>
                  <el-button v-if="['CONFIRMED', 'MEETUP_PENDING'].includes(app.status)" type="success" @click="updateOwnerApplication(app, 'IN_USE')">已交付</el-button>
                  <el-button v-if="app.status === 'IN_USE'" plain @click="updateOwnerApplication(app, 'RETURN_PENDING')">待归还</el-button>
                  <el-button v-if="app.status === 'RETURN_PENDING'" type="success" @click="updateOwnerApplication(app, 'COMPLETED')">完成归还</el-button>
                  <el-button v-if="['PENDING_OWNER_CONFIRMATION', 'NEGOTIATING', 'CONFIRMED', 'MEETUP_PENDING'].includes(app.status)" type="danger" plain @click="updateOwnerApplication(app, 'REJECTED')">拒绝/结束</el-button>
                </el-space>
                <el-timeline class="timeline">
                  <el-timeline-item v-for="node in app.timeline" :key="`${app.id}-${node.title}`" :type="getTimelineType(node.state)" :timestamp="formatDateTime(node.eventTime)">
                    <div class="timeline-row">
                      <span>{{ node.title }}</span>
                      <el-tag size="small" effect="plain">{{ node.state }}</el-tag>
                    </div>
                    <div class="muted">{{ node.description }}</div>
                  </el-timeline-item>
                </el-timeline>
              </el-card>
            </div>
          </template>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的申请" name="applications">
        <el-card shadow="never">
          <el-empty v-if="!userStore.isLoggedIn" description="登录后可以查看自己提交的租用申请">
            <el-button type="primary" @click="goLogin">去登录</el-button>
          </el-empty>
          <template v-else>
            <el-empty v-if="!marketLoading && !renterApplications.length" description="你还没有提交过个人出租申请" />
            <div v-else class="stack" v-loading="marketLoading">
              <el-card v-for="app in renterApplications" :key="app.id" shadow="never">
                <div class="card-header">
                  <div>
                    <strong>{{ app.listingTitle }}</strong>
                    <div class="muted">车主：{{ app.ownerUsername }} · {{ formatDateRange(app.requestedStartTime, app.requestedEndTime) }}</div>
                  </div>
                  <el-space wrap>
                    <el-tag :type="getApplicationStatusType(app.status)">{{ getApplicationStatusLabel(app.status) }}</el-tag>
                    <el-button text @click="consultPeer(app.ownerId, app.listingTitle)">咨询车主</el-button>
                  </el-space>
                </div>
                <p class="muted">{{ app.renterMessage || '你没有留下说明。' }}</p>
                <div class="meta-list">
                  <div>交付地点：{{ app.meetupLocation || '待确认' }}</div>
                  <div v-if="app.meetupTime">交付时间：{{ formatDateTime(app.meetupTime) }}</div>
                </div>
                <el-space wrap>
                  <el-button v-if="['PENDING_OWNER_CONFIRMATION', 'NEGOTIATING', 'CONFIRMED', 'MEETUP_PENDING'].includes(app.status)" plain @click="updateRenterApplication(app, 'CANCELLED')">取消申请</el-button>
                  <el-button v-if="app.status === 'IN_USE'" type="primary" @click="updateRenterApplication(app, 'RETURN_PENDING')">申请归还</el-button>
                </el-space>
                <el-timeline class="timeline">
                  <el-timeline-item v-for="node in app.timeline" :key="`${app.id}-${node.title}`" :type="getTimelineType(node.state)" :timestamp="formatDateTime(node.eventTime)">
                    <div class="timeline-row">
                      <span>{{ node.title }}</span>
                      <el-tag size="small" effect="plain">{{ node.state }}</el-tag>
                    </div>
                    <div class="muted">{{ node.description }}</div>
                  </el-timeline-item>
                </el-timeline>
              </el-card>
            </div>
          </template>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="listingDialogVisible" :title="editingListingId ? '编辑出租车辆' : '发布出租车辆'" width="640px">
      <el-form ref="listingFormRef" :model="listingForm" :rules="listingRules" label-width="96px">
        <el-form-item label="车辆名称" prop="name"><el-input v-model="listingForm.name" /></el-form-item>
        <el-form-item label="车辆类型" prop="type"><el-select v-model="listingForm.type"><el-option label="山地车" value="MOUNTAIN" /><el-option label="公路车" value="ROAD" /><el-option label="城市车" value="CITY" /><el-option label="电动车" value="ELECTRIC" /><el-option label="双人车" value="TANDEM" /></el-select></el-form-item>
        <el-form-item label="交付地点" prop="location"><el-input v-model="listingForm.location" /></el-form-item>
        <el-form-item label="定位坐标" prop="latitude"><el-space wrap><el-input v-model="listingForm.latitude" placeholder="纬度" /><el-input v-model="listingForm.longitude" placeholder="经度" /><el-button plain @click="fillListingLocation">使用当前位置</el-button></el-space></el-form-item>
        <el-form-item label="租金/小时"><el-input-number v-model="listingForm.pricePerHour" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="押金"><el-input-number v-model="listingForm.deposit" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="交付方式" prop="deliveryMode"><el-radio-group v-model="listingForm.deliveryMode"><el-radio-button label="OWNER_MEETUP">车主当面交付</el-radio-button><el-radio-button label="RENTER_PICKUP">租客自提</el-radio-button><el-radio-button label="PLATFORM_DEPOT">平台托管</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="可租时间" prop="availabilityRange"><el-date-picker v-model="listingForm.availabilityRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM-DD HH:mm" /></el-form-item>
        <el-form-item label="车辆图片"><el-space wrap><el-button plain @click="triggerListingImageUpload">上传图片</el-button><span class="muted">{{ listingForm.imageUrl ? '已上传图片' : '建议上传实拍图' }}</span></el-space><img v-if="listingForm.imageUrl" :src="listingForm.imageUrl" class="preview" alt="listing" /><input ref="listingImageInputRef" type="file" accept="image/*" hidden @change="handleListingImageSelected" /></el-form-item>
        <el-form-item label="补充说明"><el-input v-model="listingForm.description" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="listingDialogVisible = false">取消</el-button><el-button type="primary" :loading="listingSubmitting" @click="submitListing">保存挂牌</el-button></template>
    </el-dialog>

    <el-dialog v-model="applicationDialogVisible" title="提交租用申请" width="560px">
      <div v-if="selectedDiscoverItem" class="dialog-summary">
        <strong>{{ selectedDiscoverItem.title }}</strong>
        <div class="muted">{{ getDeliveryModeLabel(selectedDiscoverItem.deliveryMode) }} · {{ selectedDiscoverItem.location }}</div>
      </div>
      <el-form ref="applicationFormRef" :model="applicationForm" :rules="applicationRules" label-width="96px">
        <el-form-item label="租用时间" prop="requestedRange"><el-date-picker v-model="applicationForm.requestedRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM-DD HH:mm" /></el-form-item>
        <el-form-item label="交付地点" prop="meetupLocation"><el-input v-model="applicationForm.meetupLocation" /></el-form-item>
        <el-form-item label="建议时间"><el-date-picker v-model="applicationForm.meetupTime" type="datetime" format="YYYY-MM-DD HH:mm" /></el-form-item>
        <el-form-item label="给车主的话"><el-input v-model="applicationForm.renterMessage" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="applicationDialogVisible = false">取消</el-button><el-button type="primary" :loading="applicationSubmitting" @click="submitApplication">提交申请</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { consultMarketplaceListing, createMarketplaceApplication, createMarketplaceListing, getMarketplaceDiscover, getMarketplaceOwnerApplications, getMarketplaceRenterApplications, getMyMarketplaceListings, updateMarketplaceApplicationStatus, updateMarketplaceListing } from '@/api/marketplace'
import { uploadImage } from '@/api/file'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('discover')
const discoverLoading = ref(false)
const marketLoading = ref(false)
const locating = ref(false)
const listingSubmitting = ref(false)
const applicationSubmitting = ref(false)
const listingDialogVisible = ref(false)
const applicationDialogVisible = ref(false)
const editingListingId = ref(null)
const selectedDiscoverItem = ref(null)
const listingFormRef = ref(null)
const applicationFormRef = ref(null)
const listingImageInputRef = ref(null)
const discoverItems = ref([])
const myListings = ref([])
const ownerApplications = ref([])
const renterApplications = ref([])
const coords = reactive({ latitude: null, longitude: null })
const discoverFilters = reactive({ type: '', radiusKm: 8 })
const listingForm = reactive({ name: '', type: 'CITY', location: '', latitude: null, longitude: null, pricePerHour: 12, deposit: 0, deliveryMode: 'OWNER_MEETUP', availabilityRange: [], imageUrl: '', description: '', status: 'AVAILABLE' })
const applicationForm = reactive({ requestedRange: [], meetupLocation: '', meetupTime: null, renterMessage: '' })
const listingRules = { name: [{ required: true, message: '请输入车辆名称', trigger: 'blur' }], type: [{ required: true, message: '请选择车辆类型', trigger: 'change' }], location: [{ required: true, message: '请输入交付地点', trigger: 'blur' }], latitude: [{ required: true, message: '请填写或获取当前位置', trigger: 'blur' }], availabilityRange: [{ type: 'array', required: true, message: '请选择可租时间段', trigger: 'change' }] }
const applicationRules = { requestedRange: [{ type: 'array', required: true, message: '请选择租用时间', trigger: 'change' }], meetupLocation: [{ required: true, message: '请输入建议交付地点', trigger: 'blur' }] }
const currentLocationText = computed(() => coords.latitude === null ? '当前使用默认发现模式' : `已按当前位置推荐 ${discoverFilters.radiusKm} 公里内资源`)

const formatDateTime = (value) => { if (!value) return '待更新'; const date = new Date(String(value).replace(' ', 'T')); if (Number.isNaN(date.getTime())) return String(value); const pad = (n) => String(n).padStart(2, '0'); return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}` }
const formatForSubmit = (value) => { if (!value) return null; const date = value instanceof Date ? value : new Date(value); const pad = (n) => String(n).padStart(2, '0'); return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00` }
const parseDate = (value) => value ? new Date(String(value).replace(' ', 'T')) : null
const formatMoney = (value) => value === null || value === undefined ? '-' : `¥${Number(value).toFixed(2)}`
const formatDistance = (value) => `${Number(value).toFixed(2)} km`
const formatDateRange = (start, end) => `${formatDateTime(start)} 至 ${formatDateTime(end)}`
const getTypeLabel = (type) => ({ MOUNTAIN: '山地车', ROAD: '公路车', CITY: '城市车', ELECTRIC: '电动车', TANDEM: '双人车' }[type] || type || '未知车型')
const getDeliveryModeLabel = (mode) => ({ OWNER_MEETUP: '车主当面交付', RENTER_PICKUP: '租客自提', PLATFORM_DEPOT: '平台托管' }[mode] || '未设置')
const isOwnListing = (item) => item?.sourceType === 'OWNER' && String(item?.ownerId || '') === String(userStore.userId || '')
const getDiscoverSourceTagLabel = (item) => {
  if (item?.sourceType === 'PLATFORM') return '平台车'
  return isOwnListing(item) ? '我的挂牌' : '个人车'
}
const getDiscoverSourceTagType = (item) => {
  if (item?.sourceType === 'PLATFORM') return 'primary'
  return isOwnListing(item) ? 'info' : 'success'
}
const getListingStatusLabel = (status) => ({ AVAILABLE: '可出租', RESERVED: '待交付', RENTED: '租赁中', OFFLINE: '已下架' }[status] || status)
const getListingStatusType = (status) => ({ AVAILABLE: 'success', RESERVED: 'warning', RENTED: 'primary', OFFLINE: 'info' }[status] || 'info')
const getReviewStatusLabel = (status) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '待审核')
const getReviewStatusType = (status) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')
const shouldShowListingStatus = (listing) => listing.reviewStatus === 'APPROVED'
const getReviewStatusHint = (listing) => {
  if (listing.reviewStatus === 'APPROVED') {
    return listing.status === 'OFFLINE' ? '审核已通过，当前由你手动下架。' : '审核已通过，租客现在可以看到这辆车。'
  }
  if (listing.reviewStatus === 'REJECTED') {
    return '审核未通过，修改信息后再次保存会重新进入审核。'
  }
  return '管理员审核通过前，这辆车不会展示在附近可租列表里，也不会按“可出租”状态对外上架。'
}
const getApplicationStatusLabel = (status) => ({ PENDING_OWNER_CONFIRMATION: '待车主处理', NEGOTIATING: '沟通中', CONFIRMED: '已确认', MEETUP_PENDING: '待交付', IN_USE: '租赁中', RETURN_PENDING: '待归还', COMPLETED: '已完成', REJECTED: '已拒绝', CANCELLED: '已取消' }[status] || status)
const getApplicationStatusType = (status) => ({ PENDING_OWNER_CONFIRMATION: 'warning', NEGOTIATING: 'warning', CONFIRMED: 'success', MEETUP_PENDING: 'success', IN_USE: 'primary', RETURN_PENDING: 'warning', COMPLETED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[status] || 'info')
const getTimelineType = (state) => state === 'DONE' ? 'success' : state === 'PENDING' ? 'info' : 'warning'
const goLogin = () => router.push('/login')
const goToPlatformRentals = () => router.push('/bicycles')
const ensureLoggedIn = () => { if (userStore.isLoggedIn) return true; ElMessage.warning('请先登录后再继续'); goLogin(); return false }

const loadDiscover = async () => { discoverLoading.value = true; try { const params = { radiusKm: discoverFilters.radiusKm, type: discoverFilters.type || undefined }; if (coords.latitude !== null && coords.longitude !== null) { params.latitude = coords.latitude; params.longitude = coords.longitude } const res = await getMarketplaceDiscover(params); discoverItems.value = res.data || [] } finally { discoverLoading.value = false } }
const loadPrivateData = async () => { if (!userStore.isLoggedIn) return; marketLoading.value = true; try { const [listingRes, ownerRes, renterRes] = await Promise.all([getMyMarketplaceListings(), getMarketplaceOwnerApplications(), getMarketplaceRenterApplications()]); myListings.value = listingRes.data || []; ownerApplications.value = ownerRes.data || []; renterApplications.value = renterRes.data || [] } finally { marketLoading.value = false } }
const requestCurrentLocation = () => new Promise((resolve, reject) => { if (!navigator.geolocation) { reject(new Error('当前浏览器不支持定位')); return } navigator.geolocation.getCurrentPosition((position) => resolve({ latitude: Number(position.coords.latitude.toFixed(6)), longitude: Number(position.coords.longitude.toFixed(6)) }), reject, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }) })
const handleLocateNearby = async () => { locating.value = true; try { const position = await requestCurrentLocation(); coords.latitude = position.latitude; coords.longitude = position.longitude; await loadDiscover(); ElMessage.success('已经按你的当前位置刷新附近可租资源') } catch (error) { console.error(error); ElMessage.error('获取定位失败，请检查浏览器定位权限') } finally { locating.value = false } }
const fillListingLocation = async () => { locating.value = true; try { const position = await requestCurrentLocation(); listingForm.latitude = position.latitude; listingForm.longitude = position.longitude; if (!listingForm.location) listingForm.location = '当前位置'; ElMessage.success('已填入当前位置坐标') } catch (error) { console.error(error); ElMessage.error('定位失败，请手动填写坐标') } finally { locating.value = false } }

const resetListingForm = () => { editingListingId.value = null; Object.assign(listingForm, { name: '', type: 'CITY', location: '', latitude: null, longitude: null, pricePerHour: 12, deposit: 0, deliveryMode: 'OWNER_MEETUP', availabilityRange: [], imageUrl: '', description: '', status: 'AVAILABLE' }) }
const openListingDialog = (listing = null) => { if (!ensureLoggedIn()) return; resetListingForm(); if (listing) { editingListingId.value = listing.id; Object.assign(listingForm, { name: listing.name, type: listing.type, location: listing.location, latitude: listing.latitude, longitude: listing.longitude, pricePerHour: Number(listing.pricePerHour || 0), deposit: Number(listing.deposit || 0), deliveryMode: listing.deliveryMode, availabilityRange: [parseDate(listing.availableFrom), parseDate(listing.availableTo)].filter(Boolean), imageUrl: listing.imageUrl || '', description: listing.description || '', status: listing.status }) } listingDialogVisible.value = true }
const triggerListingImageUpload = () => listingImageInputRef.value?.click()
const handleListingImageSelected = async (event) => { const file = event?.target?.files?.[0]; if (!file) return; try { const res = await uploadImage(file); listingForm.imageUrl = res?.data?.url || ''; ElMessage.success('图片上传成功') } catch (error) { console.error(error); ElMessage.error('图片上传失败') } finally { if (event?.target) event.target.value = '' } }
const submitListing = async () => { if (!listingFormRef.value) return; await listingFormRef.value.validate(); listingSubmitting.value = true; try { const payload = { name: listingForm.name, type: listingForm.type, location: listingForm.location, latitude: Number(listingForm.latitude), longitude: Number(listingForm.longitude), pricePerHour: Number(listingForm.pricePerHour), deposit: Number(listingForm.deposit || 0), deliveryMode: listingForm.deliveryMode, availableFrom: formatForSubmit(listingForm.availabilityRange?.[0]), availableTo: formatForSubmit(listingForm.availabilityRange?.[1]), imageUrl: listingForm.imageUrl || null, description: listingForm.description || null, status: listingForm.status }; const res = editingListingId.value ? await updateMarketplaceListing(editingListingId.value, payload) : await createMarketplaceListing(payload); ElMessage.success(res.message || (editingListingId.value ? '挂牌更新成功' : '挂牌发布成功')); listingDialogVisible.value = false; await Promise.all([loadPrivateData(), loadDiscover()]) } finally { listingSubmitting.value = false } }

const openApplicationDialog = (item) => { if (!ensureLoggedIn()) return; selectedDiscoverItem.value = item; Object.assign(applicationForm, { requestedRange: [], meetupLocation: item.location || '', meetupTime: null, renterMessage: '' }); applicationDialogVisible.value = true }
const submitApplication = async () => { if (!applicationFormRef.value || !selectedDiscoverItem.value) return; await applicationFormRef.value.validate(); applicationSubmitting.value = true; try { await createMarketplaceApplication(selectedDiscoverItem.value.listingId, { requestedStartTime: formatForSubmit(applicationForm.requestedRange?.[0]), requestedEndTime: formatForSubmit(applicationForm.requestedRange?.[1]), meetupLocation: applicationForm.meetupLocation, meetupTime: formatForSubmit(applicationForm.meetupTime), renterMessage: applicationForm.renterMessage || null }); applicationDialogVisible.value = false; activeTab.value = 'applications'; ElMessage.success('租用申请已提交'); await Promise.all([loadPrivateData(), loadDiscover()]) } finally { applicationSubmitting.value = false } }
const consultOwner = async (item) => { if (!ensureLoggedIn()) return; try { const res = await consultMarketplaceListing(item.listingId); router.push({ path: '/friends', query: { targetUserId: String(res.data.ownerId), prefill: res.data.suggestedMessage } }) } catch (error) { console.error(error) } }
const consultPeer = (userId, listingTitle) => { if (!ensureLoggedIn()) return; router.push({ path: '/friends', query: { targetUserId: String(userId), prefill: `你好，想继续沟通一下“${listingTitle}”的交付细节。` } }); ElMessage.success('已为你打开聊天窗口') }
const updateOwnerApplication = async (application, status) => { await ElMessageBox.confirm(`确认把申请更新为“${getApplicationStatusLabel(status)}”吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }); await updateMarketplaceApplicationStatus(application.id, { status }); ElMessage.success('申请状态已更新'); await Promise.all([loadPrivateData(), loadDiscover()]) }
const updateRenterApplication = async (application, status) => { await ElMessageBox.confirm(`确认执行“${getApplicationStatusLabel(status)}”吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }); await updateMarketplaceApplicationStatus(application.id, { status }); ElMessage.success('申请状态已更新'); await Promise.all([loadPrivateData(), loadDiscover()]) }
const toggleListingStatus = async (listing) => { const nextStatus = listing.status === 'OFFLINE' ? 'AVAILABLE' : 'OFFLINE'; const res = await updateMarketplaceListing(listing.id, { name: listing.name, type: listing.type, location: listing.location, latitude: Number(listing.latitude), longitude: Number(listing.longitude), pricePerHour: Number(listing.pricePerHour), deposit: Number(listing.deposit || 0), deliveryMode: listing.deliveryMode, availableFrom: formatForSubmit(listing.availableFrom), availableTo: formatForSubmit(listing.availableTo), imageUrl: listing.imageUrl || null, description: listing.description || null, status: nextStatus }); ElMessage.success(res.message || (nextStatus === 'OFFLINE' ? '挂牌已下架' : '挂牌已重新上架')); await Promise.all([loadPrivateData(), loadDiscover()]) }

onMounted(async () => { await loadDiscover(); await loadPrivateData() })
watch(() => userStore.isLoggedIn, (loggedIn) => { if (loggedIn) { loadPrivateData().catch((error) => console.error(error)); return } myListings.value = []; ownerApplications.value = []; renterApplications.value = [] })
</script>

<style scoped>
.marketplace-page {
  --market-ink: #24343f;
  --market-muted: #6b7782;
  --market-panel: #fffdf9;
  --market-line: rgba(36, 52, 63, 0.08);
  --market-accent: #315a56;
  --market-sand: #f3ece2;
  --market-warm: #ede4d5;
  max-width: 1480px;
  margin: 0 auto;
  padding: 24px;
}

.hero-card {
  margin-bottom: 20px;
  border: 1px solid rgba(92, 79, 60, 0.08);
  background: linear-gradient(135deg, #fbf8f3 0%, #f1ebdf 100%);
}

.hero-head,
.toolbar,
.card-header,
.timeline-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.hero-kicker {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(116, 94, 63, 0.12);
  color: #745e3f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.hero-head h1 {
  margin: 12px 0 8px;
  color: var(--market-ink);
}

.hero-head p,
.muted,
.toolbar-text {
  color: var(--market-muted);
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar {
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.card-col {
  margin-bottom: 16px;
}

.resource-card :deep(.el-card__body),
.marketplace-page :deep(.el-card__body) {
  background: var(--market-panel);
}

.cover {
  width: 100%;
  height: 180px;
  border-radius: 14px;
  margin-bottom: 12px;
}

.price-line {
  font-size: 22px;
  font-weight: 800;
  color: var(--market-accent);
  margin-bottom: 8px;
}

.meta-list,
.stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-hint {
  margin: 12px 0 8px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  background: #f1ede7;
  color: #59656f;
  border: 1px solid rgba(89, 101, 111, 0.08);
}

.review-hint--approved {
  background: #eef3ef;
  color: #50655e;
  border-color: rgba(80, 101, 94, 0.08);
}

.review-hint--pending {
  background: #f5efe5;
  color: #7b6847;
  border-color: rgba(123, 104, 71, 0.08);
}

.review-hint--rejected {
  background: #f5ece9;
  color: #8c5b52;
  border-color: rgba(140, 91, 82, 0.08);
}

.review-remark {
  margin-bottom: 10px;
  font-size: 13px;
  color: #9a5b52;
}

.sub-title {
  font-weight: 700;
  margin: 22px 0 14px;
  color: var(--market-ink);
}

.timeline {
  margin-top: 14px;
}

.timeline-row {
  align-items: center;
}

.preview {
  width: 100%;
  max-width: 220px;
  border-radius: 14px;
  margin-top: 12px;
  display: block;
}

.dialog-summary {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--market-sand);
  color: var(--market-ink);
}

@media (max-width: 768px) {
  .marketplace-page {
    padding: 12px;
  }

  .hero-head,
  .toolbar,
  .card-header {
    flex-direction: column;
  }
}
</style>
