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
          <el-button type="primary" @click="loadDiscover" :loading="discoverLoading">获取附近可租</el-button>
          <el-button plain v-if="userStore.isLoggedIn" @click="openListingDialog()">我要出租</el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="附近可租" name="discover">
        <el-card shadow="never">
          <div class="toolbar">
            <el-space wrap class="region-toolbar">
              <el-dropdown trigger="click" @command="(val) => discoverFilters.type = val">
                <el-button>
                  {{ discoverFilters.type ? typeOptions.find(o => o.value === discoverFilters.type)?.label : '车型筛选' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="">全部车型</el-dropdown-item>
                    <el-dropdown-item v-for="item in typeOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-dropdown trigger="click" @command="(val) => discoverFilters.radiusKm = val">
                <el-button>
                  {{ discoverFilters.radiusKm ? discoverFilters.radiusKm + ' 公里' : '距离范围' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="3">3 公里</el-dropdown-item>
                    <el-dropdown-item :command="5">5 公里</el-dropdown-item>
                    <el-dropdown-item :command="8">8 公里</el-dropdown-item>
                    <el-dropdown-item :command="10">10 公里</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <div class="custom-select" ref="discoverProvinceRef">
                <el-button @click.stop="toggleDiscoverDropdown('province')" :disabled="discoverLoading" :class="{ 'selected': discoverRegion.provinceCode }">
                  {{ discoverRegion.provinceCode ? provinceOptions.find(o => o.value === discoverRegion.provinceCode)?.label : '选择省份' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <div v-show="discoverDropdownVisible.province" class="custom-select-dropdown">
                  <div v-for="p in provinceOptions" :key="p.value" class="custom-select-item" @click="selectDiscoverProvince(p.value)">{{ p.label }}</div>
                </div>
              </div>
              <div class="custom-select" ref="discoverCityRef">
                <el-button @click.stop="toggleDiscoverDropdown('city')" :disabled="!discoverRegion.provinceCode || discoverLoading" :class="{ 'selected': discoverRegion.cityCode }">
                  {{ discoverRegion.cityCode ? discoverCityOptions.find(o => o.value === discoverRegion.cityCode)?.label : '选择城市' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <div v-show="discoverDropdownVisible.city" class="custom-select-dropdown">
                  <div v-for="c in discoverCityOptions" :key="c.value" class="custom-select-item" @click="selectDiscoverCity(c.value)">{{ c.label }}</div>
                </div>
              </div>
              <div class="custom-select" ref="discoverDistrictRef">
                <el-button @click.stop="toggleDiscoverDropdown('district')" :disabled="!discoverRegion.cityCode || discoverLoading" :class="{ 'selected': discoverRegion.districtCode }">
                  {{ discoverRegion.districtCode ? discoverDistrictOptions.find(o => o.value === discoverRegion.districtCode)?.label : '选择区/县' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <div v-show="discoverDropdownVisible.district" class="custom-select-dropdown">
                  <div v-for="d in discoverDistrictOptions" :key="d.value" class="custom-select-item" @click="selectDiscoverDistrict(d.value)">{{ d.label }}</div>
                </div>
              </div>
              <el-button type="primary" plain @click="applyDiscoverRegion" :loading="locating" :disabled="!discoverRegion.districtCode">按所选地区推荐</el-button>
              <el-button plain @click="loadDiscover" :loading="discoverLoading">刷新推荐</el-button>
              <el-button plain @click="resetDiscoverRegion" :disabled="!hasDiscoverRegion">清空地区</el-button>
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

          <div class="pagination-wrapper" v-if="discoverTotal > discoverPageSize">
            <span class="pagination-total">共 {{ discoverTotal }} 条</span>
            <el-dropdown trigger="click" @command="(size) => { discoverPageSize = size; loadDiscover() }">
              <span class="pagination-size-trigger">
                {{ discoverPageSize }} 条/页
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="8" :class="{ active: discoverPageSize === 8 }">8 条/页</el-dropdown-item>
                  <el-dropdown-item :command="12" :class="{ active: discoverPageSize === 12 }">12 条/页</el-dropdown-item>
                  <el-dropdown-item :command="16" :class="{ active: discoverPageSize === 16 }">16 条/页</el-dropdown-item>
                  <el-dropdown-item :command="20" :class="{ active: discoverPageSize === 20 }">20 条/页</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-pagination
              v-model:current-page="discoverPage"
              v-model:page-size="discoverPageSize"
              :total="discoverTotal"
              layout="prev, pager, next"
              @current-change="loadDiscover"
            />
          </div>
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
            <div v-else v-loading="marketLoading">
              <div
                v-for="app in ownerApplications"
                :key="app.id"
                class="app-card"
                :class="{ expanded: expandedAppId === app.id }"
                @click="expandedAppId = expandedAppId === app.id ? null : app.id"
              >
                <div class="app-card-main">
                  <el-avatar :size="40" class="app-avatar">{{ app.renterUsername?.charAt(0).toUpperCase() }}</el-avatar>
                  <div class="app-card-content">
                    <div class="app-card-title">{{ app.listingTitle }}</div>
                    <div class="app-card-meta">{{ app.renterUsername }} · {{ formatDateRange(app.requestedStartTime, app.requestedEndTime) }}</div>
                  </div>
                  <div class="app-card-status">
                    <el-tag :type="getApplicationStatusType(app.status)" size="small">{{ getApplicationStatusLabel(app.status) }}</el-tag>
                    <el-icon class="app-card-arrow"><ArrowDown /></el-icon>
                  </div>
                </div>
                <el-collapse-transition>
                  <div v-if="expandedAppId === app.id" class="app-card-detail">
                    <div class="app-detail-section">
                      <div class="app-detail-label">租客说明</div>
                      <div class="app-detail-value">{{ app.renterMessage || '租客还没有留下说明。' }}</div>
                    </div>
                    <div class="app-detail-row">
                      <div class="app-detail-section">
                        <div class="app-detail-label">交付地点</div>
                        <div class="app-detail-value">{{ app.meetupLocation || '待确认' }}</div>
                      </div>
                      <div class="app-detail-section" v-if="app.meetupTime">
                        <div class="app-detail-label">交付时间</div>
                        <div class="app-detail-value">{{ formatDateTime(app.meetupTime) }}</div>
                      </div>
                    </div>
                    <div class="app-actions">
                      <el-button size="small" @click.stop="consultPeer(app.renterId, app.listingTitle)">发消息</el-button>
                      <el-button v-if="app.status === 'PENDING_OWNER_CONFIRMATION'" size="small" plain @click.stop="updateOwnerApplication(app, 'NEGOTIATING')">沟通中</el-button>
                      <el-button v-if="['PENDING_OWNER_CONFIRMATION', 'NEGOTIATING'].includes(app.status)" size="small" type="primary" @click.stop="updateOwnerApplication(app, 'CONFIRMED')">确认出租</el-button>
                      <el-button v-if="['CONFIRMED', 'MEETUP_PENDING'].includes(app.status)" size="small" type="success" @click.stop="updateOwnerApplication(app, 'IN_USE')">已交付</el-button>
                      <el-button v-if="app.status === 'IN_USE'" size="small" plain @click.stop="updateOwnerApplication(app, 'RETURN_PENDING')">待归还</el-button>
                      <el-button v-if="app.status === 'RETURN_PENDING'" size="small" type="success" @click.stop="updateOwnerApplication(app, 'COMPLETED')">完成归还</el-button>
                      <el-button v-if="['PENDING_OWNER_CONFIRMATION', 'NEGOTIATING', 'CONFIRMED', 'MEETUP_PENDING'].includes(app.status)" size="small" type="danger" plain @click.stop="updateOwnerApplication(app, 'REJECTED')">拒绝</el-button>
                    </div>
                    <el-divider v-if="app.timeline?.length" />
                    <el-timeline v-if="app.timeline?.length" class="timeline">
                      <el-timeline-item v-for="node in app.timeline" :key="`${app.id}-${node.title}`" :type="getTimelineType(node.state)" :timestamp="formatDateTime(node.eventTime)">
                        <div class="timeline-row">
                          <span>{{ node.title }}</span>
                          <el-tag size="small" effect="plain">{{ node.state }}</el-tag>
                        </div>
                        <div class="muted">{{ node.description }}</div>
                      </el-timeline-item>
                    </el-timeline>
                  </div>
                </el-collapse-transition>
              </div>
            </div>

            <div class="pagination-wrapper" v-if="ownerAppTotal > ownerAppPageSize">
              <span class="pagination-total">共 {{ ownerAppTotal }} 条</span>
              <el-dropdown trigger="click" @command="(size) => { ownerAppPageSize = size; ownerAppPage = 1; loadPrivateData() }">
                <span class="pagination-size-trigger">
                  {{ ownerAppPageSize }} 条/页
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="10" :class="{ active: ownerAppPageSize === 10 }">10 条/页</el-dropdown-item>
                    <el-dropdown-item :command="20" :class="{ active: ownerAppPageSize === 20 }">20 条/页</el-dropdown-item>
                    <el-dropdown-item :command="50" :class="{ active: ownerAppPageSize === 50 }">50 条/页</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-pagination
                v-model:current-page="ownerAppPage"
                v-model:page-size="ownerAppPageSize"
                :total="ownerAppTotal"
                layout="prev, pager, next"
                @current-change="loadPrivateData"
              />
            </div>
          </template>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的租用" name="applications">
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
        <el-form-item label="车辆类型" prop="type">
          <el-dropdown trigger="click" @command="(val) => listingForm.type = val">
            <el-button>
              {{ listingForm.type ? typeOptions.find(o => o.value === listingForm.type)?.label : '请选择车型' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in typeOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-form-item>
        <el-form-item label="交付地区" prop="districtCode">
          <el-space wrap class="region-toolbar region-toolbar--form">
            <div class="custom-select" ref="listingProvinceRef">
              <el-button @click.stop="toggleListingDropdown('province')" :class="{ 'selected': listingForm.provinceCode }">
                {{ listingForm.provinceCode ? provinceOptions.find(o => o.value === listingForm.provinceCode)?.label : '选择省份' }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <div v-show="listingDropdownVisible.province" class="custom-select-dropdown">
                <div v-for="p in provinceOptions" :key="p.value" class="custom-select-item" @click="selectListingProvince(p.value)">{{ p.label }}</div>
              </div>
            </div>
            <div class="custom-select" ref="listingCityRef">
              <el-button @click.stop="toggleListingDropdown('city')" :disabled="!listingForm.provinceCode" :class="{ 'selected': listingForm.cityCode }">
                {{ listingForm.cityCode ? listingCityOptions.find(o => o.value === listingForm.cityCode)?.label : '选择城市' }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <div v-show="listingDropdownVisible.city" class="custom-select-dropdown">
                <div v-for="c in listingCityOptions" :key="c.value" class="custom-select-item" @click="selectListingCity(c.value)">{{ c.label }}</div>
              </div>
            </div>
            <div class="custom-select" ref="listingDistrictRef">
              <el-button @click.stop="toggleListingDropdown('district')" :disabled="!listingForm.cityCode" :class="{ 'selected': listingForm.districtCode }">
                {{ listingForm.districtCode ? listingDistrictOptions.find(o => o.value === listingForm.districtCode)?.label : '选择区/县' }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <div v-show="listingDropdownVisible.district" class="custom-select-dropdown">
                <div v-for="d in listingDistrictOptions" :key="d.value" class="custom-select-item" @click="selectListingDistrict(d.value)">{{ d.label }}</div>
              </div>
            </div>
          </el-space>
        </el-form-item>
        <el-form-item v-if="listingRegionWarning" label="地点提示">
          <el-alert :title="listingRegionWarning" type="warning" :closable="false" class="region-alert" />
        </el-form-item>
        <el-form-item label="定位结果">
          <div class="location-panel">
            <div class="location-primary">{{ listingLocationText }}</div>
            <div class="muted">{{ listingCoordinateText }}</div>
          </div>
        </el-form-item>
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
      <el-alert
        title="系统会按你当前所在位置强校验，只有 10 公里范围内的车辆才能提交租用申请。"
        type="info"
        :closable="false"
        class="range-alert"
      />
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
import { onMounted, onUnmounted, nextTick, reactive, ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElIcon } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { consultMarketplaceListing, createMarketplaceApplication, createMarketplaceListing, getMarketplaceDiscover, getMarketplaceLocationHint, getMarketplaceOwnerApplications, getMarketplaceRenterApplications, getMyMarketplaceListings, updateMarketplaceApplicationStatus, updateMarketplaceListing } from '@/api/marketplace'
import { uploadImage } from '@/api/file'
import { chinaRegionOptions } from '@/data/chinaRegionOptions'

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
const activeDiscoverRegionText = ref('')
const activeDiscoverSourceText = ref('')
const listingRegionWarning = ref('')
const selectedDiscoverItem = ref(null)
const expandedAppId = ref(null)
const listingFormRef = ref(null)
const applicationFormRef = ref(null)
const listingImageInputRef = ref(null)
const discoverItems = ref([])
const discoverTotal = ref(0)
const discoverPage = ref(1)
const discoverPageSize = ref(12)
const myListings = ref([])
const ownerApplications = ref([])
const ownerAppTotal = ref(0)
const ownerAppPage = ref(1)
const ownerAppPageSize = ref(10)
const renterApplications = ref([])
const coords = reactive({ latitude: null, longitude: null })
const discoverFilters = reactive({ type: '', radiusKm: 8 })
const discoverRegion = reactive({ provinceCode: '', cityCode: '', districtCode: '' })
const listingForm = reactive({ name: '', type: 'CITY', provinceCode: '', cityCode: '', districtCode: '', location: '', latitude: null, longitude: null, pricePerHour: 12, deposit: 0, deliveryMode: 'OWNER_MEETUP', availabilityRange: [], imageUrl: '', description: '', status: 'AVAILABLE' })
const listingDropdownVisible = reactive({ province: false, city: false, district: false })
const discoverDropdownVisible = reactive({ province: false, city: false, district: false })
const listingProvinceRef = ref(null)
const listingCityRef = ref(null)
const listingDistrictRef = ref(null)
const discoverProvinceRef = ref(null)
const discoverCityRef = ref(null)
const discoverDistrictRef = ref(null)
const applicationForm = reactive({ requestedRange: [], meetupLocation: '', meetupTime: null, renterMessage: '' })
const listingRules = { name: [{ required: true, message: '请输入车辆名称', trigger: 'blur' }], type: [{ required: true, message: '请选择车辆类型', trigger: 'change' }], districtCode: [{ required: true, message: '请选择完整的省/市/区', trigger: 'change' }], availabilityRange: [{ type: 'array', required: true, message: '请选择可租时间段', trigger: 'change' }] }
const applicationRules = { requestedRange: [{ type: 'array', required: true, message: '请选择租用时间', trigger: 'change' }], meetupLocation: [{ required: true, message: '请输入建议交付地点', trigger: 'blur' }] }
const typeOptions = [
  { label: '山地车', value: 'MOUNTAIN' },
  { label: '公路车', value: 'ROAD' },
  { label: '城市车', value: 'CITY' },
  { label: '电动车', value: 'ELECTRIC' },
  { label: '双人车', value: 'TANDEM' }
]
const provinceOptions = chinaRegionOptions
const hasDiscoverRegion = computed(() => Boolean(discoverRegion.provinceCode || discoverRegion.cityCode || discoverRegion.districtCode))
const discoverCityOptions = computed(() => getCityOptions(discoverRegion.provinceCode))
const discoverDistrictOptions = computed(() => getDistrictOptions(discoverRegion.provinceCode, discoverRegion.cityCode))
const listingCityOptions = computed(() => getCityOptions(listingForm.provinceCode))
const listingDistrictOptions = computed(() => getDistrictOptions(listingForm.provinceCode, listingForm.cityCode))
const currentLocationText = computed(() => {
  if (activeDiscoverRegionText.value) {
    const sourceSuffix = activeDiscoverSourceText.value ? `（${activeDiscoverSourceText.value}）` : ''
    return `已按 ${activeDiscoverRegionText.value}${sourceSuffix} 推荐 ${discoverFilters.radiusKm} 公里内资源，超出 10 公里不可租用`
  }
  const regionText = getRegionLabelText(discoverRegion.provinceCode, discoverRegion.cityCode, discoverRegion.districtCode)
  if (regionText) return `已选中 ${regionText}，点击“按所选地区推荐”后刷新附近资源`
  if (hasDiscoverRegion.value) return '请选择到区/县后再按地区推荐'
  return ''
})
const listingLocationText = computed(() => listingForm.location || '请选择中国省/市/区，系统会自动生成标准交付地点')
const listingCoordinateText = computed(() => listingForm.latitude === null || listingForm.longitude === null ? '系统会根据你选中的区/县中心点自动写入经纬度' : `经度 ${Number(listingForm.longitude).toFixed(6)} · 纬度 ${Number(listingForm.latitude).toFixed(6)}`)

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
const getProvinceNode = (provinceCode) => provinceOptions.find((item) => item.value === String(provinceCode || '')) || null
const getCityOptions = (provinceCode) => getProvinceNode(provinceCode)?.children || []
const getCityNode = (provinceCode, cityCode) => getCityOptions(provinceCode).find((item) => item.value === String(cityCode || '')) || null
const getDistrictOptions = (provinceCode, cityCode) => getCityNode(provinceCode, cityCode)?.children || []
const getDistrictNode = (provinceCode, cityCode, districtCode) => getDistrictOptions(provinceCode, cityCode).find((item) => item.value === String(districtCode || '')) || null
const joinRegionLabels = (labels) => labels.filter((label, index) => label && label !== labels[index - 1]).join(' ')
const getRegionLabelText = (provinceCode, cityCode, districtCode) => {
  const province = getProvinceNode(provinceCode)
  const city = getCityNode(provinceCode, cityCode)
  const district = getDistrictNode(provinceCode, cityCode, districtCode)
  return joinRegionLabels([province?.label, city?.label, district?.label])
}
const normalizeRegionText = (value) => String(value || '').replace(/[\s,，/、.\-]/g, '')
const normalizeAreaName = (value) => String(value || '')
  .replace(/\s+/g, '')
  .replace(/特别行政区|自治区|自治州|自治县|地区|盟/g, '')
  .replace(/省|市|区|县/g, '')
  .replace(/壮族|回族|维吾尔|蒙古族|土家族|苗族|藏族|朝鲜族自治/g, '')
const findRegionSelectionByLocation = (location) => {
  const normalized = normalizeRegionText(location)
  if (!normalized) return null
  for (const province of provinceOptions) {
    for (const city of province.children || []) {
      for (const district of city.children || []) {
        const labels = [province.label, city.label, district.label]
        const normalizedLabels = labels.map((label) => normalizeRegionText(label))
        const fullName = normalizedLabels.join('')
        const matchesByContain = normalizedLabels.every((label) => normalized.includes(label))
        if (normalized === fullName || normalized.includes(fullName) || matchesByContain) {
          return { provinceCode: province.value, cityCode: city.value, districtCode: district.value }
        }
      }
    }
  }
  return null
}
const matchesAreaName = (label, target) => {
  const normalizedLabel = normalizeAreaName(label)
  const normalizedTarget = normalizeAreaName(target)
  if (!normalizedLabel || !normalizedTarget) return false
  return normalizedLabel === normalizedTarget
    || normalizedLabel.includes(normalizedTarget)
    || normalizedTarget.includes(normalizedLabel)
}
const measureCoordinateDistance = (latitudeA, longitudeA, latitudeB, longitudeB) => {
  if ([latitudeA, longitudeA, latitudeB, longitudeB].some((value) => value === null || value === undefined)) {
    return Number.MAX_SAFE_INTEGER
  }
  const latDelta = Number(latitudeA) - Number(latitudeB)
  const lonDelta = Number(longitudeA) - Number(longitudeB)
  return Math.sqrt(latDelta * latDelta + lonDelta * lonDelta)
}
const pickBestDistrictCandidate = (candidates, latitude, longitude) => candidates
  .slice()
  .sort((left, right) => {
    if (right.score !== left.score) {
      return right.score - left.score
    }
    const leftDistance = measureCoordinateDistance(left.district.latitude, left.district.longitude, latitude, longitude)
    const rightDistance = measureCoordinateDistance(right.district.latitude, right.district.longitude, latitude, longitude)
    return leftDistance - rightDistance
  })[0] || null
const resolveRegionFromLocationHint = (hint) => {
  if (!hint) return null
  if (hint.countryCode && hint.countryCode !== 'CN') {
    return null
  }
  const provinceName = hint.province || ''
  const cityName = hint.city || ''
  const districtName = hint.district || ''
  const latitude = hint.latitude
  const longitude = hint.longitude
  const candidates = []

  for (const province of provinceOptions) {
    const provinceMatched = !provinceName || matchesAreaName(province.label, provinceName)
    for (const city of province.children || []) {
      const cityMatched = !cityName || matchesAreaName(city.label, cityName)
      for (const district of city.children || []) {
        const districtMatched = !districtName || matchesAreaName(district.label, districtName)
        if ((provinceName && !provinceMatched) || (cityName && !cityMatched) || (districtName && !districtMatched)) {
          continue
        }

        let score = 0
        if (provinceMatched && provinceName) score += 1
        if (cityMatched && cityName) score += 3
        if (districtMatched && districtName) score += 6

        candidates.push({ province, city, district, score })
      }
    }
  }

  const bestCandidate = candidates.length
    ? pickBestDistrictCandidate(candidates, latitude, longitude)
    : pickBestDistrictCandidate(
      provinceOptions.flatMap((province) => (province.children || []).flatMap((city) => (city.children || []).map((district) => ({
        province,
        city,
        district,
        score: 0
      })))),
      latitude,
      longitude
    )

  if (!bestCandidate) return null
  return {
    provinceCode: bestCandidate.province.value,
    cityCode: bestCandidate.city.value,
    districtCode: bestCandidate.district.value,
    latitude: bestCandidate.district.latitude,
    longitude: bestCandidate.district.longitude,
    label: joinRegionLabels([bestCandidate.province.label, bestCandidate.city.label, bestCandidate.district.label])
  }
}
const buildLocationHintText = (hint) => {
  if (!hint) return ''
  return joinRegionLabels([hint.country, hint.province, hint.city, hint.district]) || hint.locationText || hint.ip || '当前位置'
}
const syncListingRegionSelection = () => {
  const district = getDistrictNode(listingForm.provinceCode, listingForm.cityCode, listingForm.districtCode)
  if (!district) {
    listingForm.location = ''
    listingForm.latitude = null
    listingForm.longitude = null
    return
  }
  listingForm.location = getRegionLabelText(listingForm.provinceCode, listingForm.cityCode, listingForm.districtCode)
  listingForm.latitude = district.latitude
  listingForm.longitude = district.longitude
  listingRegionWarning.value = ''
}

const loadDiscover = async () => { discoverLoading.value = true; try { const params = { radiusKm: discoverFilters.radiusKm, type: discoverFilters.type || undefined, page: discoverPage.value, size: discoverPageSize.value }; if (coords.latitude !== null && coords.longitude !== null) { params.latitude = coords.latitude; params.longitude = coords.longitude } const res = await getMarketplaceDiscover(params); discoverItems.value = res.data.content || []; discoverTotal.value = res.data.totalElements || 0 } finally { discoverLoading.value = false } }
const loadPrivateData = async () => { if (!userStore.isLoggedIn) return; marketLoading.value = true; try { const [listingRes, ownerRes, renterRes] = await Promise.all([getMyMarketplaceListings(), getMarketplaceOwnerApplications({ page: ownerAppPage.value, size: ownerAppPageSize.value }), getMarketplaceRenterApplications()]); myListings.value = listingRes.data || []; ownerApplications.value = ownerRes.data.content || []; ownerAppTotal.value = ownerRes.data.totalElements || 0; renterApplications.value = renterRes.data || [] } finally { marketLoading.value = false } }
const initializeDiscoverBySilentLocation = async () => {
  locating.value = true
  try {
    const res = await getMarketplaceLocationHint()
    const hint = res?.data
    if (!hint || hint.latitude === null || hint.latitude === undefined || hint.longitude === null || hint.longitude === undefined) {
      activeDiscoverRegionText.value = ''
      activeDiscoverSourceText.value = ''
      await loadDiscover()
      return
    }
    const matchedRegion = resolveRegionFromLocationHint(hint)
    if (matchedRegion) {
      discoverRegion.provinceCode = matchedRegion.provinceCode
      discoverRegion.cityCode = matchedRegion.cityCode
      discoverRegion.districtCode = matchedRegion.districtCode
      coords.latitude = matchedRegion.latitude
      coords.longitude = matchedRegion.longitude
      activeDiscoverRegionText.value = matchedRegion.label
      activeDiscoverSourceText.value = '中国 IP 静默定位'
      await loadDiscover()
      return
    }

    discoverRegion.provinceCode = ''
    discoverRegion.cityCode = ''
    discoverRegion.districtCode = ''
    coords.latitude = Number(hint.latitude)
    coords.longitude = Number(hint.longitude)
    activeDiscoverRegionText.value = buildLocationHintText(hint)
    activeDiscoverSourceText.value = '全球 IP 静默定位'
    await loadDiscover()
  } catch (error) {
    console.error(error)
    activeDiscoverRegionText.value = ''
    activeDiscoverSourceText.value = ''
    await loadDiscover()
  } finally {
    locating.value = false
  }
}
const handleDiscoverProvinceChange = () => {
  discoverRegion.cityCode = ''
  discoverRegion.districtCode = ''
  coords.latitude = null
  coords.longitude = null
  activeDiscoverRegionText.value = ''
  activeDiscoverSourceText.value = ''
}
const handleDiscoverCityChange = () => {
  discoverRegion.districtCode = ''
  coords.latitude = null
  coords.longitude = null
  activeDiscoverRegionText.value = ''
  activeDiscoverSourceText.value = ''
}
const handleDiscoverDistrictChange = () => {
  coords.latitude = null
  coords.longitude = null
  activeDiscoverRegionText.value = ''
  activeDiscoverSourceText.value = ''
}
const applyDiscoverRegion = async () => {
  const district = getDistrictNode(discoverRegion.provinceCode, discoverRegion.cityCode, discoverRegion.districtCode)
  if (!district) {
    ElMessage.warning('请先选择完整的省/市/区')
    return
  }
  const regionText = getRegionLabelText(discoverRegion.provinceCode, discoverRegion.cityCode, discoverRegion.districtCode)
  locating.value = true
  try {
    coords.latitude = district.latitude
    coords.longitude = district.longitude
    activeDiscoverRegionText.value = regionText
    activeDiscoverSourceText.value = '手动选择'
    discoverPage.value = 1
    await loadDiscover()
    ElMessage.success(`已经按 ${regionText} 刷新附近可租资源`)
  } finally {
    locating.value = false
  }
}
const resetDiscoverRegion = async () => {
  discoverRegion.provinceCode = ''
  discoverRegion.cityCode = ''
  discoverRegion.districtCode = ''
  coords.latitude = null
  coords.longitude = null
  activeDiscoverRegionText.value = ''
  activeDiscoverSourceText.value = ''
  discoverPage.value = 1
  await loadDiscover()
}
const toggleListingDropdown = (key) => {
  if (key === 'province') {
    if (listingDropdownVisible.province) {
      listingDropdownVisible.province = false
    } else {
      listingDropdownVisible.province = false
      listingDropdownVisible.city = false
      listingDropdownVisible.district = false
      listingDropdownVisible.province = true
    }
  } else if (key === 'city') {
    if (listingDropdownVisible.city) {
      listingDropdownVisible.city = false
    } else {
      listingDropdownVisible.province = false
      listingDropdownVisible.city = false
      listingDropdownVisible.district = false
      if (listingForm.provinceCode) {
        listingDropdownVisible.city = true
      }
    }
  } else if (key === 'district') {
    if (listingDropdownVisible.district) {
      listingDropdownVisible.district = false
    } else {
      listingDropdownVisible.province = false
      listingDropdownVisible.city = false
      listingDropdownVisible.district = false
      if (listingForm.cityCode) {
        listingDropdownVisible.district = true
      }
    }
  }
}
const closeAllListingDropdowns = (e) => {
  if (!listingProvinceRef.value?.contains(e?.target) && !listingCityRef.value?.contains(e?.target) && !listingDistrictRef.value?.contains(e?.target)) {
    listingDropdownVisible.province = false
    listingDropdownVisible.city = false
    listingDropdownVisible.district = false
  }
}
const selectListingProvince = (val) => {
  listingForm.provinceCode = val
  listingForm.cityCode = ''
  listingForm.districtCode = ''
  listingForm.location = ''
  listingForm.latitude = null
  listingForm.longitude = null
  listingDropdownVisible.province = false
}
const selectListingCity = (val) => {
  listingForm.cityCode = val
  listingForm.districtCode = ''
  listingForm.location = ''
  listingForm.latitude = null
  listingForm.longitude = null
  listingDropdownVisible.city = false
}
const selectListingDistrict = (val) => {
  listingForm.districtCode = val
  listingDropdownVisible.district = false
  syncListingRegionSelection()
}
const toggleDiscoverDropdown = (key) => {
  if (key === 'province') {
    if (discoverDropdownVisible.province) {
      discoverDropdownVisible.province = false
    } else {
      discoverDropdownVisible.province = false
      discoverDropdownVisible.city = false
      discoverDropdownVisible.district = false
      if (!discoverLoading.value) {
        discoverDropdownVisible.province = true
      }
    }
  } else if (key === 'city') {
    if (discoverDropdownVisible.city) {
      discoverDropdownVisible.city = false
    } else {
      discoverDropdownVisible.province = false
      discoverDropdownVisible.city = false
      discoverDropdownVisible.district = false
      if (discoverRegion.provinceCode && !discoverLoading.value) {
        discoverDropdownVisible.city = true
      }
    }
  } else if (key === 'district') {
    if (discoverDropdownVisible.district) {
      discoverDropdownVisible.district = false
    } else {
      discoverDropdownVisible.province = false
      discoverDropdownVisible.city = false
      discoverDropdownVisible.district = false
      if (discoverRegion.cityCode && !discoverLoading.value) {
        discoverDropdownVisible.district = true
      }
    }
  }
}
const closeAllDiscoverDropdowns = (e) => {
  if (!discoverProvinceRef.value?.contains(e?.target) && !discoverCityRef.value?.contains(e?.target) && !discoverDistrictRef.value?.contains(e?.target)) {
    discoverDropdownVisible.province = false
    discoverDropdownVisible.city = false
    discoverDropdownVisible.district = false
  }
}
const selectDiscoverProvince = (val) => {
  discoverRegion.provinceCode = val
  discoverRegion.cityCode = ''
  discoverRegion.districtCode = ''
  discoverDropdownVisible.province = false
}
const selectDiscoverCity = (val) => {
  discoverRegion.cityCode = val
  discoverRegion.districtCode = ''
  discoverDropdownVisible.city = false
}
const selectDiscoverDistrict = (val) => {
  discoverRegion.districtCode = val
  discoverDropdownVisible.district = false
  handleFilter()
}
const handleListingProvinceChange = () => {
  listingForm.cityCode = ''
  listingForm.districtCode = ''
  listingForm.location = ''
  listingForm.latitude = null
  listingForm.longitude = null
}
const handleListingCityChange = () => {
  listingForm.districtCode = ''
  listingForm.location = ''
  listingForm.latitude = null
  listingForm.longitude = null
}
const handleListingDistrictChange = () => syncListingRegionSelection()
const resetListingForm = () => {
  editingListingId.value = null
  listingRegionWarning.value = ''
  Object.assign(listingForm, { name: '', type: 'CITY', provinceCode: '', cityCode: '', districtCode: '', location: '', latitude: null, longitude: null, pricePerHour: 12, deposit: 0, deliveryMode: 'OWNER_MEETUP', availabilityRange: [], imageUrl: '', description: '', status: 'AVAILABLE' })
}
const openListingDialog = (listing = null) => {
  if (!ensureLoggedIn()) return
  resetListingForm()
  if (listing) {
    editingListingId.value = listing.id
    Object.assign(listingForm, { name: listing.name, type: listing.type, location: listing.location, latitude: listing.latitude, longitude: listing.longitude, pricePerHour: Number(listing.pricePerHour || 0), deposit: Number(listing.deposit || 0), deliveryMode: listing.deliveryMode, availabilityRange: [parseDate(listing.availableFrom), parseDate(listing.availableTo)].filter(Boolean), imageUrl: listing.imageUrl || '', description: listing.description || '', status: listing.status })
    const matchedRegion = findRegionSelectionByLocation(listing.location)
    if (matchedRegion) {
      Object.assign(listingForm, matchedRegion)
      syncListingRegionSelection()
    } else if (listing.location) {
      listingRegionWarning.value = '这条旧挂牌的地点不是标准省/市/区格式，请重新选择完整地区后再保存。'
    }
  }
  listingDialogVisible.value = true
}
const triggerListingImageUpload = () => listingImageInputRef.value?.click()
const handleListingImageSelected = async (event) => { const file = event?.target?.files?.[0]; if (!file) return; try { const res = await uploadImage(file); listingForm.imageUrl = res?.data?.url || ''; ElMessage.success('图片上传成功') } catch (error) { console.error(error); ElMessage.error('图片上传失败') } finally { if (event?.target) event.target.value = '' } }
const submitListing = async () => { if (!listingFormRef.value) return; await listingFormRef.value.validate(); listingSubmitting.value = true; try { const payload = { name: listingForm.name, type: listingForm.type, location: listingForm.location, latitude: Number(listingForm.latitude), longitude: Number(listingForm.longitude), pricePerHour: Number(listingForm.pricePerHour), deposit: Number(listingForm.deposit || 0), deliveryMode: listingForm.deliveryMode, availableFrom: formatForSubmit(listingForm.availabilityRange?.[0]), availableTo: formatForSubmit(listingForm.availabilityRange?.[1]), imageUrl: listingForm.imageUrl || null, description: listingForm.description || null, status: listingForm.status }; const res = editingListingId.value ? await updateMarketplaceListing(editingListingId.value, payload) : await createMarketplaceListing(payload); ElMessage.success(res.message || (editingListingId.value ? '挂牌更新成功' : '挂牌发布成功')); listingDialogVisible.value = false; await Promise.all([loadPrivateData(), loadDiscover()]) } finally { listingSubmitting.value = false } }

const openApplicationDialog = (item) => { if (!ensureLoggedIn()) return; selectedDiscoverItem.value = item; Object.assign(applicationForm, { requestedRange: [], meetupLocation: item.location || '', meetupTime: null, renterMessage: '' }); applicationDialogVisible.value = true }
const submitApplication = async () => { if (!applicationFormRef.value || !selectedDiscoverItem.value) return; await applicationFormRef.value.validate(); applicationSubmitting.value = true; try { await createMarketplaceApplication(selectedDiscoverItem.value.listingId, { requestedStartTime: formatForSubmit(applicationForm.requestedRange?.[0]), requestedEndTime: formatForSubmit(applicationForm.requestedRange?.[1]), meetupLocation: applicationForm.meetupLocation, meetupTime: formatForSubmit(applicationForm.meetupTime), renterMessage: applicationForm.renterMessage || null }); applicationDialogVisible.value = false; activeTab.value = 'applications'; ElMessage.success('租用申请已提交'); await Promise.all([loadPrivateData(), loadDiscover()]) } finally { applicationSubmitting.value = false } }
const consultOwner = async (item) => { if (!ensureLoggedIn()) return; try { const res = await consultMarketplaceListing(item.listingId); router.push({ path: '/friends', query: { targetUserId: String(res.data.ownerId), prefill: res.data.suggestedMessage } }) } catch (error) { console.error(error) } }

const consultPeer = (userId, listingTitle) => { if (!ensureLoggedIn()) return; router.push({ path: '/friends', query: { targetUserId: String(userId), prefill: `你好，想继续沟通一下”${listingTitle}”的交付细节。` } }); ElMessage.success('已为你打开聊天窗口') }
const updateOwnerApplication = async (application, status) => { await ElMessageBox.confirm(`确认把申请更新为“${getApplicationStatusLabel(status)}”吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }); await updateMarketplaceApplicationStatus(application.id, { status }); ElMessage.success('申请状态已更新'); await Promise.all([loadPrivateData(), loadDiscover()]) }
const updateRenterApplication = async (application, status) => { await ElMessageBox.confirm(`确认执行“${getApplicationStatusLabel(status)}”吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }); await updateMarketplaceApplicationStatus(application.id, { status }); ElMessage.success('申请状态已更新'); await Promise.all([loadPrivateData(), loadDiscover()]) }
const toggleListingStatus = async (listing) => { const nextStatus = listing.status === 'OFFLINE' ? 'AVAILABLE' : 'OFFLINE'; const res = await updateMarketplaceListing(listing.id, { name: listing.name, type: listing.type, location: listing.location, latitude: Number(listing.latitude), longitude: Number(listing.longitude), pricePerHour: Number(listing.pricePerHour), deposit: Number(listing.deposit || 0), deliveryMode: listing.deliveryMode, availableFrom: formatForSubmit(listing.availableFrom), availableTo: formatForSubmit(listing.availableTo), imageUrl: listing.imageUrl || null, description: listing.description || null, status: nextStatus }); ElMessage.success(res.message || (nextStatus === 'OFFLINE' ? '挂牌已下架' : '挂牌已重新上架')); await Promise.all([loadPrivateData(), loadDiscover()]) }

onMounted(async () => {
  await initializeDiscoverBySilentLocation()
  await loadPrivateData()
  document.addEventListener('click', closeAllListingDropdowns)
  document.addEventListener('click', closeAllDiscoverDropdowns)
})
onUnmounted(() => {
  document.removeEventListener('click', closeAllListingDropdowns)
  document.removeEventListener('click', closeAllDiscoverDropdowns)
})
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

.region-toolbar {
  row-gap: 10px;
}

.region-toolbar--form {
  width: 100%;
}

.custom-select {
  position: relative;
  overflow: visible;
}

.custom-select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 9999;
  min-width: 160px;
  max-height: 240px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-top: 4px;
  transform: none;
}

.custom-select-item {
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.custom-select-item:hover {
  background: #f5f7fa;
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

.range-alert {
  margin-bottom: 14px;
}

.region-alert {
  width: 100%;
}

.location-panel {
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--market-sand);
  border: 1px solid rgba(36, 52, 63, 0.08);
}

.location-primary {
  font-weight: 700;
  color: var(--market-ink);
  margin-bottom: 6px;
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

  .hero-actions {
    flex-direction: column;
    width: 100%;
  }

  .hero-actions .el-button {
    width: 100%;
  }

  .toolbar {
    display: flex;
    flex-wrap: nowrap !important;
    overflow-x: auto;
    overflow-y: visible;
    -webkit-overflow-scrolling: touch;
    gap: 12px;
    align-items: center;
    position: relative;
  }

  .region-toolbar {
    flex-wrap: nowrap !important;
    justify-content: flex-start;
    min-width: max-content;
    padding-bottom: 4px;
    overflow: visible;
    position: relative;
    z-index: 1;
  }

  .custom-select {
    position: relative;
    overflow: visible;
    z-index: 10;
  }

  .custom-select-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    z-index: 9999;
    min-width: 160px;
    max-height: 240px;
    overflow-y: auto;
    background: #fff;
    border: 1px solid #e4e4e7;
    border-radius: 4px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    margin-top: 4px;
  }

  .toolbar-text {
    white-space: nowrap;
  }

  .el-tabs__content {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    overflow-y: visible;
  }

  .el-tab-pane > .el-card {
    overflow: visible;
  }

  .el-tab-pane {
    overflow: visible;
  }

  .el-tab-pane {
    min-width: 100%;
  }

  .marketplace-page {
    overflow-x: hidden;
    overflow-y: visible;
  }
}

/* ========== 黑夜模式 ========== */
html.dark .marketplace-page {
  --market-ink: #f1f5f9;
  --market-muted: #94a3b8;
  --market-panel: rgba(30, 41, 59, 0.60);
  --market-line: rgba(148, 163, 184, 0.20);
  --market-accent: #fdba74;
  --market-sand: rgba(30, 41, 59, 0.50);
  --market-warm: rgba(51, 65, 85, 0.60);
}

html.dark .hero-card {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.80) 0%, rgba(15, 23, 42, 0.85) 100%);
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .hero-kicker {
  background: rgba(255, 107, 53, 0.15);
  color: #fdba74;
}

html.dark .hero-head h1,
html.dark .card-header strong,
html.dark .sub-title,
html.dark .location-primary {
  color: #ffffff;
}

html.dark .hero-head p,
html.dark .muted,
html.dark .toolbar-text,
html.dark .review-remark {
  color: #cbd5e1;
}

html.dark .resource-card :deep(.el-card__body),
html.dark .marketplace-page :deep(.el-card__body) {
  background: rgba(30, 41, 59, 0.50);
}

html.dark .price-line {
  color: #fdba74;
}

html.dark .review-hint {
  background: rgba(51, 65, 85, 0.50);
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.15);
}

html.dark .review-hint--approved {
  background: rgba(20, 83, 45, 0.30);
  color: #6ee7b7;
  border-color: rgba(52, 211, 153, 0.20);
}

html.dark .review-hint--pending {
  background: rgba(120, 53, 15, 0.25);
  color: #fcd34d;
  border-color: rgba(251, 191, 36, 0.20);
}

html.dark .review-hint--rejected {
  background: rgba(127, 29, 29, 0.25);
  color: #fca5a5;
  border-color: rgba(248, 113, 113, 0.20);
}

html.dark .dialog-summary {
  background: rgba(30, 41, 59, 0.60);
  color: #f1f5f9;
}

html.dark .location-panel {
  background: rgba(30, 41, 59, 0.60);
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-card) {
  background: rgba(15, 23, 42, 0.80);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.90);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
}

html.dark :deep(.el-input__inner) {
  color: #ffffff;
}

html.dark :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
}

html.dark :deep(.el-select .el-input__wrapper) {
  background: rgba(30, 41, 59, 0.60);
}

html.dark :deep(.el-tag--info) {
  background: rgba(148, 163, 184, 0.25);
  color: #cbd5e1;
  border: 1px solid rgba(148, 163, 184, 0.40);
  backdrop-filter: blur(12px) saturate(180%);
}

html.dark :deep(.el-tag--success) {
  background: rgba(34, 197, 94, 0.25);
  color: #86efac;
  border: 1px solid rgba(74, 222, 128, 0.40);
  backdrop-filter: blur(12px) saturate(180%);
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.25);
  color: #fcd34d;
  border: 1px solid rgba(251, 191, 36, 0.40);
  backdrop-filter: blur(12px) saturate(180%);
}

html.dark :deep(.el-tag--primary) {
  background: rgba(255, 107, 53, 0.25);
  color: #fdba74;
  border: 1px solid rgba(255, 107, 53, 0.40);
  backdrop-filter: blur(12px) saturate(180%);
}

html.dark :deep(.el-tag--danger) {
  background: rgba(239, 68, 68, 0.25);
  color: #fca5a5;
  border: 1px solid rgba(248, 113, 113, 0.40);
  backdrop-filter: blur(12px) saturate(180%);
}

html.dark :deep(.el-button--primary) {
  background: var(--brand-primary);
  border: none;
}

html.dark :deep(.el-button--primary:hover) {
  background: #ff7b4a;
}

html.dark :deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.20);
  color: #e2e8f0;
}

html.dark :deep(.el-button--default:hover) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(203, 213, 225, 0.30);
  color: #ffffff;
}

html.dark :deep(.el-button--success) {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.30);
  color: #34d399;
}

html.dark :deep(.el-button--warning) {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.30);
  color: #fbbf24;
}

html.dark :deep(.el-button--danger) {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.30);
  color: #f87171;
}

html.dark .timeline :deep(.el-timeline-item__node) {
  border-color: rgba(148, 163, 184, 0.30);
}

html.dark :deep(.el-divider) {
  background: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-button--text) {
  color: #fdba74;
}

html.dark :deep(.el-button--text:hover) {
  color: #fb923c;
}

html.dark .application-card {
  background: rgba(30, 41, 59, 0.60);
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .application-card:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.4);
}

html.dark .app-card-detail {
  border-top-color: rgba(148, 163, 184, 0.20);
}

/* 简洁版申请卡片 */
.app-card {
  background: var(--bs-surface);
  border: 1px solid var(--bs-stroke);
  border-radius: 12px;
  margin-bottom: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.app-card:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.3);
}

.app-card.expanded {
  border-color: var(--brand-primary);
}

.app-card-main {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.app-avatar {
  background: var(--brand-primary);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.app-card-content {
  flex: 1;
  min-width: 0;
}

.app-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-card-meta {
  font-size: 12px;
  color: var(--bs-muted);
  margin-top: 2px;
}

.app-card-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.app-card-arrow {
  color: var(--bs-muted);
  transition: transform 0.2s;
}

.app-card.expanded .app-card-arrow {
  transform: rotate(180deg);
}

.app-card-detail {
  padding: 0 16px 16px;
  border-top: 1px solid var(--bs-stroke);
  margin-top: 0;
}

.app-detail-section {
  margin-top: 12px;
}

.app-detail-row {
  display: flex;
  gap: 24px;
}

.app-detail-label {
  font-size: 12px;
  color: var(--bs-muted);
  margin-bottom: 2px;
}

.app-detail-value {
  font-size: 14px;
  color: var(--bs-ink);
}

.app-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

/* 申请卡片样式 */
.timeline {
  margin-top: 14px;
  padding-left: 4px;
}

.timeline :deep(.el-timeline-item) {
  margin-bottom: 0;
  padding-bottom: 16px;
}

.timeline :deep(.el-timeline-item__timestamp) {
  color: var(--market-muted);
  font-size: 12px;
}

.timeline-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.timeline-row span {
  font-weight: 500;
  color: var(--market-ink);
}

/* 时间轴节点 - 简洁圆点 */
.timeline :deep(.el-timeline-item__node) {
  background: #cbd5e1;
  border: none !important;
  width: 10px;
  height: 10px;
}

.timeline :deep(.el-timeline-item__node.is-success),
.timeline :deep(.el-timeline-item__node.is-warning) {
  background: #3b82f6 !important;
  animation: nodeActive 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

@keyframes nodeActive {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  60% {
    transform: scale(1.4);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.timeline :deep(.el-timeline-item__node.is-info) {
  background: #cbd5e1;
}

.timeline :deep(.el-timeline-item__node.is-primary) {
  background: #3b82f6;
}

.timeline :deep(.el-timeline-item__node.is-danger) {
  background: #ef4444;
}

/* 时间轴内容区域 */
.timeline :deep(.el-timeline-item__content) {
  color: var(--market-ink);
  font-size: 14px;
  line-height: 1.6;
}

/* 时间轴项包装器 - 流动波浪动画 */
.timeline :deep(.el-timeline-item__wrapper) {
  padding: 12px 16px;
  margin-left: 8px;
  border-radius: 12px;
  border-left: 3px solid transparent;
  opacity: 0;
  transform: translateY(-30px);
  animation: waveFlow 0.8s ease-out forwards;
}

@keyframes waveFlow {
  0% {
    opacity: 0;
    transform: translateY(-30px) scale(0.95);
  }
  50% {
    opacity: 0.7;
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 每个时间轴项延迟流动 - 形成波浪效果 */
.timeline :deep(.el-timeline-item:nth-child(1)) .el-timeline-item__wrapper { animation-delay: 0s; }
.timeline :deep(.el-timeline-item:nth-child(2)) .el-timeline-item__wrapper { animation-delay: 0.15s; }
.timeline :deep(.el-timeline-item:nth-child(3)) .el-timeline-item__wrapper { animation-delay: 0.3s; }
.timeline :deep(.el-timeline-item:nth-child(4)) .el-timeline-item__wrapper { animation-delay: 0.45s; }
.timeline :deep(.el-timeline-item:nth-child(5)) .el-timeline-item__wrapper { animation-delay: 0.6s; }
.timeline :deep(.el-timeline-item:nth-child(6)) .el-timeline-item__wrapper { animation-delay: 0.75s; }

/* 当前进行中步骤 */
.timeline :deep(.el-timeline-item__wrapper:has(.is-warning)) {
  border-left-color: #f59e0b;
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.08), transparent);
}

/* 已完成步骤 */
.timeline :deep(.el-timeline-item__wrapper:has(.is-success)) {
  border-left-color: #10b981;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.06), transparent);
}

/* 连接线 */
.timeline :deep(.el-timeline-item__tail) {
  border-left: 2px solid rgba(59, 130, 246, 0.25);
}

.timeline :deep(.el-timeline-item__tail.is-hidden) {
  border-left-color: transparent;
}

/* 深色模式时间轴节点 */
html.dark .timeline :deep(.el-timeline-item__node) {
  background: #475569;
  border: none !important;
  width: 10px;
  height: 10px;
}

html.dark .timeline :deep(.el-timeline-item__node.is-success),
html.dark .timeline :deep(.el-timeline-item__node.is-warning) {
  background: #60a5fa !important;
  animation: nodeActive 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

html.dark .timeline :deep(.el-timeline-item__node.is-info) {
  background: #475569;
}

html.dark .timeline :deep(.el-timeline-item__node.is-primary) {
  background: #60a5fa;
}

html.dark .timeline :deep(.el-timeline-item__node.is-danger) {
  background: #f87171;
}

html.dark .timeline :deep(.el-timeline-item__content) {
  color: #e2e8f0;
}

html.dark .timeline :deep(.el-timeline-item__wrapper:has(.is-warning)) {
  border-left-color: #fcd34d;
  background: linear-gradient(90deg, rgba(251, 191, 36, 0.1), transparent);
}

html.dark .timeline :deep(.el-timeline-item__wrapper:has(.is-success)) {
  border-left-color: #6ee7b7;
  background: linear-gradient(90deg, rgba(52, 211, 153, 0.08), transparent);
}

/* 黑夜模式下拉框样式 */
html.dark .custom-select-dropdown {
  background: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

html.dark .custom-select-item {
  color: #e2e8f0;
}

html.dark .custom-select-item:hover {
  background: rgba(64, 158, 255, 0.15);
  color: #ffffff;
}

html.dark .custom-select .el-button {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(255, 255, 255, 0.15);
  color: #e2e8f0;
}

html.dark .custom-select .el-button:hover {
  background: rgba(51, 65, 85, 0.9);
  border-color: rgba(64, 158, 255, 0.5);
}

html.dark .custom-select .el-button.selected {
  border-color: rgba(64, 158, 255, 0.6);
  color: #60a5fa;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.pagination-total {
  font-size: 14px;
  color: var(--bs-muted);
}

.pagination-size-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  font-size: 14px;
  color: var(--bs-ink);
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-size-trigger:hover {
  background: rgba(15, 23, 42, 0.04);
}

html.dark .pagination-size-trigger {
  background: rgba(30, 41, 59, 0.60);
  border-color: rgba(148, 163, 184, 0.20);
  color: #e2e8f0;
}

html.dark .pagination-size-trigger:hover {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .pagination-total {
  color: #94a3b8;
}
</style>
