<template>
  <div class="dish-mgt-container" :class="{ 'dish-mgt-container--sorting': sorting }">
    <!-- 1. 顶部操作栏与筛选区 -->
    <div class="top-bar">
      <div class="page-title">{{ pageTitle }}</div>
      <div class="actions">
        <!-- 筛选器：标签式按钮组 -->
        <div class="filter-tabs">
           <div 
             class="filter-tab" 
             :class="{ active: filterStatus === 'all', disabled: sorting }"
             @click="!sorting && (filterStatus = 'all')"
           >
             全部
           </div>
           <div 
             class="filter-tab" 
             :class="{ active: filterStatus === 'on', disabled: sorting }"
             @click="!sorting && (filterStatus = 'on')"
           >
             上架中
           </div>
           <div 
             class="filter-tab" 
             :class="{ active: filterStatus === 'off', disabled: sorting }"
             @click="!sorting && (filterStatus = 'off')"
           >
             已下架
           </div>
        </div>
        
        <div class="action-buttons">
          <div class="category-filter">
            <van-popover v-model:show="showCategoryPopover" placement="bottom-start" :offset="[0, 8]" :disabled="sorting">
              <div class="popover-scroll-container">
                <div 
                  v-for="opt in catOptions" 
                  :key="opt.value"
                  class="popover-item"
                  :class="{ active: filterCatId === opt.value }"
                  @click="onSelectCategory(opt)"
                >
                  {{ opt.text }}
                </div>
              </div>
              <template #reference>
                <div class="category-trigger-btn" :class="{ disabled: sorting }">
                   <span class="trigger-text">{{ currentCategoryText }}</span>
                   <van-icon name="arrow-down" class="trigger-icon" :class="{ open: showCategoryPopover }" />
                </div>
              </template>
            </van-popover>
          </div>
          <button class="sort-mode-btn" :disabled="sorting" @click="startSorting">
            <van-icon name="sort" /> 调整顺序
          </button>
          <button class="add-btn" :disabled="sorting" @click="onAdd">
            <van-icon name="plus" /> {{ addButtonText }}
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 菜品列表（核心区域） -->
    <div class="dish-list">
      <!-- 空状态 -->
      <div v-if="!loading && filteredDishes.length === 0" class="empty-state">
        <van-empty 
          image="default" 
          :description="emptyDescription" 
        />
      </div>

      <!-- 骨架屏 -->
      <div v-if="loading" class="skeleton-list">
         <van-skeleton title avatar :row="3" v-for="i in 3" :key="i" class="mb-4" />
      </div>

      <!-- 列表内容 -->
      <template v-if="!sorting">
      <van-swipe-cell
        v-for="dish in filteredDishes" 
        :key="dish.id" 
        class="dish-card-wrapper"
      >
        <div class="dish-card">
          <div class="dish-thumb">
            <van-image 
              width="80" 
              height="80" 
              radius="8" 
              fit="cover" 
              :src="dish.image || 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'" 
            />
            <div class="status-badge" :class="dish.status === 1 ? 'on' : 'off'">
              {{ dish.status === 1 ? '上架中' : '已下架' }}
            </div>
          </div>
          
          <div class="dish-info">
            <div class="info-header">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-cat" v-if="getCategoryName(dish.categoryId)">
                {{ getCategoryName(dish.categoryId) }}
              </div>
            </div>
            <div
              v-if="!isWeightSelection && dish.optionEnabled === 1 && dish.optionSummary"
              class="dish-option-summary"
            >
              {{ dish.optionSummary }}
            </div>
            <div class="info-footer">
              <div class="dish-price" v-if="!isWeightSelection">
                <span class="currency">¥</span>{{ dish.price }}
              </div>
              <div class="dish-price weight-meta" v-else>
                {{ dish.unitLabel || '份' }} / {{ dish.referenceWeightG || 0 }}g
              </div>
              <div class="slide-tip">
                <van-icon name="arrow-left" /> 左滑管理
              </div>
            </div>
          </div>
        </div>

        <template #right>
          <div class="swipe-actions">
            <button class="swipe-btn edit" @click="onEdit(dish)">
              <van-icon name="edit" />
              <span>编辑</span>
            </button>
            <button 
              class="swipe-btn status" 
              :class="dish.status === 1 ? 'off' : 'on'"
              @click="toggleStatus(dish)"
            >
              <van-icon :name="dish.status === 1 ? 'down' : 'up'" />
              <span>{{ dish.status === 1 ? '下架' : '上架' }}</span>
            </button>
            <button class="swipe-btn delete" @click="onDelete(dish)">
              <van-icon name="delete-o" />
              <span>删除</span>
            </button>
          </div>
        </template>
      </van-swipe-cell>
      </template>

      <draggable
        v-else
        v-model="sortDraft"
        item-key="id"
        handle=".dish-sort-handle"
        :animation="180"
        ghost-class="dish-sort-card--ghost"
        chosen-class="dish-sort-card--chosen"
        drag-class="dish-sort-card--dragging"
      >
        <template #item="{ element: dish, index }">
          <div class="dish-sort-card">
            <div class="dish-sort-card__position">{{ index + 1 }}</div>
            <van-image
              width="52"
              height="52"
              radius="6"
              fit="cover"
              :src="dish.image || 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'"
            />
            <div class="dish-sort-card__info">
              <div class="dish-sort-card__name">{{ dish.name }}</div>
              <div class="dish-sort-card__meta">
                <span :class="dish.status === 1 ? 'is-on' : 'is-off'">
                  {{ dish.status === 1 ? '上架中' : '已下架' }}
                </span>
              </div>
            </div>
            <button type="button" class="dish-sort-handle" :aria-label="`拖动${dish.name}`">
              <van-icon name="wap-nav" />
            </button>
          </div>
        </template>
      </draggable>
    </div>

    <div v-if="sorting" class="sort-action-bar">
      <button type="button" class="sort-action-bar__cancel" :disabled="sortSaving" @click="cancelSorting">取消</button>
      <div class="sort-action-bar__title">{{ currentCategoryText }}</div>
      <button type="button" class="sort-action-bar__save" :disabled="sortSaving" @click="saveSorting">
        {{ sortSaving ? '保存中...' : '保存顺序' }}
      </button>
    </div>

    <!-- 3. 新增/编辑菜品抽屉 -->
    <van-popup 
      v-model:show="showEdit" 
      position="right" 
      :style="{ width: '100%', height: '100%' }"
      class="edit-drawer"
    >
      <div class="drawer-header">
        <div class="drawer-title">{{ editingId ? editTitle : addTitle }}</div>
        <div class="drawer-close" @click="showEdit = false">
           <van-icon name="cross" />
        </div>
      </div>
      
      <div class="drawer-content">
        <van-form @submit="onSave" ref="formRef">
          <div class="form-section">
            <div class="section-title">基本信息</div>
            
            <van-field
              v-model="form.name"
              name="name"
              :label="nameLabel"
              :placeholder="namePlaceholder"
              :rules="[{ required: true, message: '请填写名称' }]"
            />
            
            <van-field
              v-model="form.categoryId"
              name="categoryId"
              label="所属分类"
              :rules="[{ required: true, message: '请选择分类' }]"
            >
              <template #input>
                <div class="cat-select-wrapper">
                  <select v-model="form.categoryId" class="custom-select">
                    <option :value="undefined" disabled>请选择分类</option>
                    <option v-for="opt in catOptions.slice(1)" :key="opt.value" :value="opt.value">
                      {{ opt.text }}
                    </option>
                  </select>
                  <van-icon name="arrow-down" class="select-arrow" />
                </div>
              </template>
            </van-field>

            <van-field
              v-if="!isWeightSelection"
              v-model="form.price"
              type="number"
              name="price"
              label="价格"
              placeholder="0.00"
              :rules="[{ required: true, message: '请填写价格' }]"
            >
              <template #left-icon>
                <span class="price-prefix">¥</span>
              </template>
            </van-field>

            <template v-if="!isWeightSelection">
              <van-field label="启用规格">
                <template #input>
                  <van-switch
                    :model-value="form.optionEnabled === 1"
                    size="22px"
                    @update:model-value="(value) => handleOptionEnabledChange(value)"
                  />
                </template>
              </van-field>

              <div v-if="form.optionEnabled === 1" class="option-binding-section">
                <div class="option-binding-section__header">
                  <div>
                    <div class="option-binding-section__title">已绑定规格组</div>
                    <div class="option-binding-section__desc">支持设置必选和展示顺序</div>
                  </div>
                  <button type="button" class="inline-manage-btn" @click="openBindingPopup">
                    选择规格组
                  </button>
                </div>

                <div v-if="bindingDraft.length === 0" class="option-binding-empty">
                  暂未绑定规格组，请先从模板中选择
                </div>

                <div v-for="(binding, index) in bindingDraft" :key="binding.optionGroupId" class="option-binding-card">
                  <div class="option-binding-card__main">
                    <div class="option-binding-card__name">{{ binding.groupName }}</div>
                    <div class="option-binding-card__meta">顺序 {{ index + 1 }}</div>
                  </div>
                  <div class="option-binding-card__actions">
                    <label class="binding-required-switch">
                      <span>必选</span>
                      <van-switch
                        :model-value="binding.required === 1"
                        size="20px"
                        @update:model-value="(value) => updateBindingRequired(index, value)"
                      />
                    </label>
                    <div class="binding-sort-actions">
                      <button type="button" class="binding-sort-btn" :disabled="index === 0" @click="moveBinding(index, -1)">上移</button>
                      <button type="button" class="binding-sort-btn" :disabled="index === bindingDraft.length - 1" @click="moveBinding(index, 1)">下移</button>
                      <button type="button" class="binding-remove-btn" @click="removeBinding(index)">移除</button>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <van-field
              v-if="isWeightSelection"
              v-model="form.unitLabel"
              name="unitLabel"
              label="单位"
              placeholder="如：个 / 份 / 片"
              :rules="[{ required: true, message: '请填写单位' }]"
            />

            <van-field
              v-if="isWeightSelection"
              v-model="form.referenceWeightG"
              type="digit"
              name="referenceWeightG"
              label="参考重量"
              placeholder="请输入克重，如 30"
              :rules="[{ required: true, message: '请填写参考重量' }]"
            >
              <template #button>
                <span class="weight-suffix">g</span>
              </template>
            </van-field>
          </div>

          <div class="form-section">
            <div class="section-title">图片与展示</div>
            <van-field name="image" :label="imageLabel">
              <template #input>
                <van-uploader 
                  v-model="fileList" 
                  :max-count="1" 
                  :after-read="afterRead"
                  :max-size="DISH_IMAGE_MAX_SIZE"
                  @oversize="onOversize"
                />
              </template>
            </van-field>
          </div>

          <div class="form-actions">
             <button type="button" class="btn-cancel" @click="showEdit = false">取消</button>
             <button type="submit" class="btn-submit">确定提交</button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup
      v-model:show="showOptionGroupManager"
      position="bottom"
      round
      :style="{ height: '85vh' }"
      class="group-manager-popup"
    >
      <div class="sheet-header">
        <div class="sheet-title">规格模板管理</div>
        <div class="sheet-close" @click="closeOptionGroupManager">
          <van-icon name="cross" />
        </div>
      </div>

      <div class="sheet-body">
        <div class="sheet-toolbar">
          <button class="sheet-add-btn" @click="startCreateGroup">
            <van-icon name="plus" /> 新增规格组
          </button>
        </div>

        <div v-if="optionGroups.length === 0" class="sheet-empty">
          暂无规格模板，可先新增“份量”“辣度”“主食类型”等模板
        </div>

        <div v-for="group in optionGroups" :key="group.id" class="group-card">
          <div class="group-card__header">
            <div>
              <div class="group-card__name">{{ group.name }}</div>
              <div class="group-card__sub">单选规格组</div>
            </div>
            <div class="group-card__actions">
              <button class="group-card__btn" @click="startEditGroup(group)">编辑</button>
              <button class="group-card__btn group-card__btn--danger" @click="deleteOptionGroup(group)">删除</button>
            </div>
          </div>
          <div class="group-card__values">
            <span v-for="value in group.values || []" :key="value.id" class="group-value-tag">
              {{ value.name }}<template v-if="Number(value.extraPrice || 0) > 0"> +{{ Number(value.extraPrice).toFixed(2) }}</template>
            </span>
          </div>
        </div>

        <div v-if="groupEditorVisible" class="group-editor-card">
          <div class="group-editor-card__title">{{ editingGroupId ? '编辑规格组' : '新增规格组' }}</div>
          <van-field v-model="groupForm.name" label="规格组名称" placeholder="如：份量、辣度" />

          <div class="group-editor-values">
            <div v-for="(value, index) in groupForm.values" :key="value.localKey" class="group-editor-value-row">
              <input v-model.trim="value.name" class="group-editor-input" placeholder="规格值名称" />
              <input v-model="value.extraPrice" class="group-editor-input group-editor-input--price" type="number" placeholder="加价" />
              <label class="group-editor-default">
                <input type="checkbox" :checked="value.isDefault === 1" @change="setDefaultValue(index)" />
                <span>默认</span>
              </label>
              <button class="group-editor-remove" @click="removeGroupValue(index)">删除</button>
            </div>
          </div>

          <div class="group-editor-footer">
            <button class="group-editor-secondary" @click="addGroupValue">新增规格值</button>
            <button class="group-editor-secondary" @click="cancelGroupEditor">取消</button>
            <button class="group-editor-primary" @click="saveOptionGroup">保存</button>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup
      v-model:show="showBindingPopup"
      position="bottom"
      round
      :style="{ height: '70vh' }"
      class="binding-popup"
    >
      <div class="sheet-header">
        <div class="sheet-title">选择规格组</div>
        <div class="sheet-close" @click="showBindingPopup = false">
          <van-icon name="cross" />
        </div>
      </div>

      <div class="sheet-body">
        <div v-if="optionGroups.length === 0" class="sheet-empty">
          还没有规格模板，请先到“规格模板”里新增
        </div>
        <div
          v-for="group in optionGroups"
          :key="group.id"
          class="binding-select-card"
          :class="{ active: isBindingSelected(group.id) }"
          @click="toggleBindingSelection(group)"
        >
          <div class="binding-select-card__main">
            <div class="binding-select-card__name">{{ group.name }}</div>
            <div class="binding-select-card__values">
              {{ (group.values || []).map((value: any) => value.name).join(' / ') }}
            </div>
          </div>
          <van-icon :name="isBindingSelected(group.id) ? 'checked' : 'circle'" :color="isBindingSelected(group.id) ? '#1a8cff' : '#cbd5e1'" />
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import draggable from 'vuedraggable';
import request from '@/utils/request';
import { showSuccessToast, showConfirmDialog, showFailToast } from 'vant';

const DISH_IMAGE_MAX_SIZE = 500 * 1024;

interface DishForm {
  categoryId: number | undefined;
  name: string;
  price: string | number;
  optionEnabled: number;
  unitLabel: string;
  referenceWeightG: number | string;
  image: string;
}

interface OptionValueDraft {
  localKey: string;
  id?: number;
  name: string;
  extraPrice: string | number;
  isDefault: number;
  sort: number;
}

interface OptionGroupItem {
  id: number;
  name: string;
  selectType: string;
  values: Array<{
    id: number;
    optionGroupId: number;
    name: string;
    extraPrice: string | number;
    isDefault: number;
    sort: number;
  }>;
}

interface BindingDraftItem {
  id?: number;
  optionGroupId: number;
  groupName: string;
  required: number;
  sort: number;
  values: OptionGroupItem['values'];
}

const dishes = ref<any[]>([]);
const catOptions = ref<any[]>([{ text: '全部分类', value: 0 }]);
const filterCatId = ref(0);
const filterStatus = ref<'all' | 'on' | 'off'>('all');
const showEdit = ref(false);
const showCategoryPopover = ref(false);
const editingId = ref<number | null>(null);
const loading = ref(true);
const sorting = ref(false);
const sortSaving = ref(false);
const sortDraft = ref<any[]>([]);
const fileList = ref<any[]>([]);
const formRef = ref();
const shopConfig = ref<any>({
  shopMode: 'fixed_dish',
  weightPricePer500g: null,
  minimumOrderWeightG: 0
});

const showOptionGroupManager = ref(false);
const showBindingPopup = ref(false);
const optionGroups = ref<OptionGroupItem[]>([]);
const bindingDraft = ref<BindingDraftItem[]>([]);
const groupEditorVisible = ref(false);
const editingGroupId = ref<number | null>(null);
const groupForm = ref<{
  name: string;
  values: OptionValueDraft[];
}>({
  name: '',
  values: []
});

const createDefaultForm = (): DishForm => ({
  categoryId: undefined,
  name: '',
  price: '',
  optionEnabled: 0,
  unitLabel: '份',
  referenceWeightG: '',
  image: ''
});

const form = ref<DishForm>(createDefaultForm());

const isWeightSelection = computed(() => shopConfig.value.shopMode === 'weight_selection');
const pageTitle = computed(() => isWeightSelection.value ? '食材管理' : '菜品管理');
const addButtonText = computed(() => isWeightSelection.value ? '新增食材' : '新增菜品');
const addTitle = computed(() => isWeightSelection.value ? '新增食材' : '新增菜品');
const editTitle = computed(() => isWeightSelection.value ? '编辑食材' : '编辑菜品');
const nameLabel = computed(() => isWeightSelection.value ? '食材名称' : '菜品名称');
const namePlaceholder = computed(() => isWeightSelection.value ? '请输入食材名称' : '请输入菜品名称');
const imageLabel = computed(() => isWeightSelection.value ? '食材图片' : '菜品图片');
const emptyDescription = computed(() => isWeightSelection.value ? '暂无食材，点击右上角添加第一份食材吧！' : '暂无菜品，点击右上角添加您的第一道菜吧！');

const currentCategoryText = computed(() => {
  const found = catOptions.value.find((c) => c.value === filterCatId.value);
  return found ? found.text : '全部分类';
});

const filteredDishes = computed(() => {
  let list = dishes.value;
  if (filterStatus.value === 'on') {
    list = list.filter((d) => d.status === 1);
  } else if (filterStatus.value === 'off') {
    list = list.filter((d) => d.status === 0);
  }
  return list;
});

const createLocalKey = () => `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

const createGroupValueDraft = (value?: Partial<OptionValueDraft>): OptionValueDraft => ({
  localKey: createLocalKey(),
  id: value?.id,
  name: value?.name || '',
  extraPrice: value?.extraPrice ?? '0',
  isDefault: value?.isDefault === 1 ? 1 : 0,
  sort: value?.sort ?? 0
});

const resetBindingDraft = () => {
  bindingDraft.value = [];
};

const resetGroupEditor = () => {
  editingGroupId.value = null;
  groupEditorVisible.value = false;
  groupForm.value = {
    name: '',
    values: [createGroupValueDraft({ isDefault: 1, extraPrice: '0' })]
  };
};

const normalizeBindingDraft = (bindings: any[] = []): BindingDraftItem[] => {
  return [...bindings]
    .sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0))
    .map((binding, index) => ({
      id: binding.id,
      optionGroupId: Number(binding.optionGroupId),
      groupName: binding.groupName || binding.name || '',
      required: Number(binding.required) === 0 ? 0 : 1,
      sort: index + 1,
      values: Array.isArray(binding.values) ? binding.values : []
    }));
};

const getCategoryName = (id: number) => {
  const cat = catOptions.value.find((c) => c.value === id);
  return cat ? cat.text : '';
};

const fetchShopConfig = async () => {
  const res: any = await request.get('/api/merchant/shop');
  shopConfig.value = {
    shopMode: res.shopMode || 'fixed_dish',
    weightPricePer500g: res.weightPricePer500g,
    minimumOrderWeightG: res.minimumOrderWeightG || 0
  };
};

const fetchCategories = async () => {
  try {
    const res: any = await request.get('/api/merchant/categories');
    catOptions.value = [
      { text: '全部分类', value: 0 },
      ...res.map((c: any) => ({ text: c.name, value: c.id }))
    ];
  } catch (e) {
    console.error(e);
  }
};

const fetchOptionGroups = async () => {
  if (isWeightSelection.value) {
    optionGroups.value = [];
    return;
  }
  const res: any = await request.get('/api/merchant/dish-option-groups');
  optionGroups.value = Array.isArray(res) ? res : [];
};

const fetchDishOptionBindings = async (dishId: number) => {
  if (isWeightSelection.value) {
    resetBindingDraft();
    return;
  }
  const res: any = await request.get(`/api/merchant/dishes/${dishId}/option-bindings`);
  bindingDraft.value = normalizeBindingDraft(Array.isArray(res) ? res : []);
  form.value.optionEnabled = bindingDraft.value.length > 0 ? 1 : 0;
};

const onSelectCategory = (opt: any) => {
  if (sorting.value) return;
  filterCatId.value = opt.value;
  showCategoryPopover.value = false;
  fetchDishes();
};

const fetchDishes = async () => {
  loading.value = true;
  try {
    const params: any = {};
    if (filterCatId.value !== 0) params.categoryId = filterCatId.value;
    const url = isWeightSelection.value ? '/api/merchant/weight-ingredients' : '/api/merchant/dishes';
    const res: any = await request.get(url, { params });
    dishes.value = Array.isArray(res) ? res : [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const afterRead = async (file: any) => {
  if (file?.file?.size > DISH_IMAGE_MAX_SIZE) {
    showFailToast('图片大小不能超过 500KB');
    fileList.value = [];
    return;
  }

  file.status = 'uploading';
  file.message = '上传中...';

  const formData = new FormData();
  formData.append('file', file.file);

  try {
    const res: any = await request.post('/api/merchant/upload/dish-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });

    form.value.image = res.url;
    file.status = 'done';
    file.message = '上传成功';
    file.url = res.url;
  } catch (e) {
    file.status = 'failed';
    file.message = '上传失败';
    showFailToast('图片上传失败');
    fileList.value = [];
  }
};

const onOversize = () => {
  showFailToast('图片大小不能超过 500KB');
};

const handleOptionEnabledChange = async (value: boolean) => {
  if (value) {
    form.value.optionEnabled = 1;
    if (!optionGroups.value.length) {
      await fetchOptionGroups();
    }
    return;
  }
  form.value.optionEnabled = 0;
  bindingDraft.value = [];
};

const updateBindingRequired = (index: number, value: boolean) => {
  const current = bindingDraft.value[index];
  if (!current) return;
  current.required = value ? 1 : 0;
};

const moveBinding = (index: number, offset: number) => {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= bindingDraft.value.length) return;
  const next = [...bindingDraft.value];
  const [current] = next.splice(index, 1);
  if (!current) return;
  next.splice(targetIndex, 0, current);
  bindingDraft.value = next.map((item, idx) => ({
    ...item,
    sort: idx + 1
  }));
};

const removeBinding = (index: number) => {
  bindingDraft.value.splice(index, 1);
  bindingDraft.value = bindingDraft.value.map((item, idx) => ({
    ...item,
    sort: idx + 1
  }));
  if (bindingDraft.value.length === 0) {
    form.value.optionEnabled = 0;
  }
};

const openOptionGroupManager = async () => {
  await fetchOptionGroups();
  resetGroupEditor();
  showOptionGroupManager.value = true;
};

const closeOptionGroupManager = () => {
  showOptionGroupManager.value = false;
  resetGroupEditor();
};

const startCreateGroup = () => {
  editingGroupId.value = null;
  groupEditorVisible.value = true;
  groupForm.value = {
    name: '',
    values: [createGroupValueDraft({ isDefault: 1, extraPrice: '0' })]
  };
};

const startEditGroup = (group: OptionGroupItem) => {
  editingGroupId.value = group.id;
  groupEditorVisible.value = true;
  groupForm.value = {
    name: group.name,
    values: (group.values || []).map((value, index) =>
      createGroupValueDraft({
        id: value.id,
        name: value.name,
        extraPrice: value.extraPrice,
        isDefault: Number(value.isDefault) === 1 ? 1 : 0,
        sort: Number(value.sort || index + 1)
      })
    )
  };
  if (groupForm.value.values.length === 0) {
    groupForm.value.values = [createGroupValueDraft({ isDefault: 1, extraPrice: '0' })];
  }
};

const cancelGroupEditor = () => {
  resetGroupEditor();
};

const addGroupValue = () => {
  groupForm.value.values.push(createGroupValueDraft({
    isDefault: groupForm.value.values.length === 0 ? 1 : 0,
    sort: groupForm.value.values.length + 1,
    extraPrice: '0'
  }));
};

const removeGroupValue = (index: number) => {
  if (groupForm.value.values.length === 1) {
    showFailToast('至少保留一个规格值');
    return;
  }
  const removed = groupForm.value.values[index];
  groupForm.value.values.splice(index, 1);
  if (removed?.isDefault === 1 && groupForm.value.values.length > 0) {
    const firstValue = groupForm.value.values[0];
    if (firstValue) {
      firstValue.isDefault = 1;
    }
  }
  groupForm.value.values = groupForm.value.values.map((item, idx) => ({
    ...item,
    sort: idx + 1
  }));
};

const setDefaultValue = (index: number) => {
  groupForm.value.values = groupForm.value.values.map((item, idx) => ({
    ...item,
    isDefault: idx === index ? 1 : 0
  }));
};

const validateGroupForm = () => {
  if (!groupForm.value.name.trim()) {
    showFailToast('请填写规格组名称');
    return false;
  }
  if (groupForm.value.values.length === 0) {
    showFailToast('请至少添加一个规格值');
    return false;
  }
  const nameSet = new Set<string>();
  for (const value of groupForm.value.values) {
    const valueName = value.name.trim();
    if (!valueName) {
      showFailToast('规格值名称不能为空');
      return false;
    }
    if (nameSet.has(valueName)) {
      showFailToast('同一规格组内不能有重复规格值');
      return false;
    }
    nameSet.add(valueName);
  }
  return true;
};

const saveOptionGroup = async () => {
  if (!validateGroupForm()) return;
  const payload = {
    name: groupForm.value.name.trim(),
    selectType: 'single',
    values: groupForm.value.values.map((value, index) => ({
      id: value.id,
      name: value.name.trim(),
      extraPrice: Number(value.extraPrice || 0).toFixed(2),
      isDefault: value.isDefault === 1 ? 1 : 0,
      sort: index + 1
    }))
  };
  try {
    if (editingGroupId.value) {
      await request.put(`/api/merchant/dish-option-groups/${editingGroupId.value}`, payload);
    } else {
      await request.post('/api/merchant/dish-option-groups', payload);
    }
    showSuccessToast(editingGroupId.value ? '规格组已更新' : '规格组已新增');
    await fetchOptionGroups();
    resetGroupEditor();
  } catch (e) {
    console.error(e);
  }
};

const deleteOptionGroup = async (group: OptionGroupItem) => {
  try {
    const bindingCountRes: any = await request.get(`/api/merchant/dish-option-groups/${group.id}/binding-count`);
    const bindingCount = Number(bindingCountRes?.count || 0);
    const message = bindingCount > 0
      ? `规格组“${group.name}”当前已被 ${bindingCount} 个菜品绑定，删除后这些菜品将失去对应规格，确认继续吗？`
      : `确定删除规格组“${group.name}”吗？删除后不可恢复。`;
    await showConfirmDialog({
      title: '确认删除规格组',
      message,
      confirmButtonColor: '#ff4d4f'
    });
    await request.delete(`/api/merchant/dish-option-groups/${group.id}`);
    showSuccessToast('规格组已删除');
    await fetchOptionGroups();
    bindingDraft.value = bindingDraft.value.filter((item) => item.optionGroupId !== group.id);
    if (bindingDraft.value.length === 0) {
      form.value.optionEnabled = 0;
    }
  } catch (e) {
    console.error(e);
  }
};

const openBindingPopup = async () => {
  if (optionGroups.value.length === 0) {
    await fetchOptionGroups();
  }
  showBindingPopup.value = true;
};

const isBindingSelected = (groupId: number) =>
  bindingDraft.value.some((item) => item.optionGroupId === Number(groupId));

const toggleBindingSelection = (group: OptionGroupItem) => {
  const groupId = Number(group.id);
  const existingIndex = bindingDraft.value.findIndex((item) => item.optionGroupId === groupId);
  if (existingIndex >= 0) {
    bindingDraft.value.splice(existingIndex, 1);
    bindingDraft.value = bindingDraft.value.map((item, idx) => ({
      ...item,
      sort: idx + 1
    }));
  } else {
    bindingDraft.value.push({
      optionGroupId: groupId,
      groupName: group.name,
      required: 1,
      sort: bindingDraft.value.length + 1,
      values: group.values || []
    });
    form.value.optionEnabled = 1;
  }
};

const buildBindingPayload = () =>
  bindingDraft.value.map((item, index) => ({
    optionGroupId: item.optionGroupId,
    required: item.required === 0 ? 0 : 1,
    sort: index + 1
  }));

const onAdd = () => {
  if (sorting.value) return;
  editingId.value = null;
  form.value = createDefaultForm();
  fileList.value = [];
  resetBindingDraft();
  showEdit.value = true;
};

const onEdit = async (dish: any) => {
  editingId.value = dish.id;
  form.value = {
    categoryId: dish.categoryId,
    name: dish.name || '',
    price: dish.price ?? '',
    optionEnabled: Number(dish.optionEnabled) === 1 ? 1 : 0,
    unitLabel: dish.unitLabel || '份',
    referenceWeightG: dish.referenceWeightG ?? '',
    image: dish.image || ''
  };

  fileList.value = dish.image ? [{ url: dish.image, isImage: true }] : [];
  bindingDraft.value = normalizeBindingDraft(dish.optionBindings || []);
  showEdit.value = true;

  if (!isWeightSelection.value) {
    await fetchOptionGroups();
    await fetchDishOptionBindings(dish.id);
  }
};

const onDelete = (dish: any) => {
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除${isWeightSelection.value ? '食材' : '菜品'}“${dish.name}”吗？此操作无法撤销。`,
    confirmButtonColor: '#ff4d4f'
  }).then(async () => {
    await request.delete(`${isWeightSelection.value ? '/api/merchant/weight-ingredients' : '/api/merchant/dishes'}/${dish.id}`);
    showSuccessToast('删除成功');
    fetchDishes();
  }).catch(() => {});
};

const validateDishForm = () => {
  if (!form.value.name.trim()) {
    showFailToast(`请填写${isWeightSelection.value ? '食材' : '菜品'}名称`);
    return false;
  }
  if (!form.value.categoryId) {
    showFailToast('请选择分类');
    return false;
  }
  if (!isWeightSelection.value) {
    if (form.value.price === '' || Number(form.value.price) < 0) {
      showFailToast('请填写有效价格');
      return false;
    }
    if (form.value.optionEnabled === 1 && bindingDraft.value.length === 0) {
      showFailToast('已启用规格时，至少绑定一个规格组');
      return false;
    }
  } else {
    if (!String(form.value.unitLabel || '').trim()) {
      showFailToast('请填写单位');
      return false;
    }
    if (!Number(form.value.referenceWeightG || 0)) {
      showFailToast('请填写参考重量');
      return false;
    }
  }
  return true;
};

const onSave = async () => {
  if (!validateDishForm()) return;
  try {
    const payload: any = { ...form.value };
    delete payload.sort;
    if (!isWeightSelection.value) {
      payload.optionEnabled = form.value.optionEnabled === 1 ? 1 : 0;
      delete payload.unitLabel;
      delete payload.referenceWeightG;
    } else {
      delete payload.price;
      delete payload.optionEnabled;
    }

    const baseUrl = isWeightSelection.value ? '/api/merchant/weight-ingredients' : '/api/merchant/dishes';
    let savedDish: any;
    if (editingId.value) {
      savedDish = await request.put(`${baseUrl}/${editingId.value}`, payload);
    } else {
      savedDish = await request.post(baseUrl, payload);
    }

    const targetDishId = Number(savedDish?.id || editingId.value || 0);
    if (!isWeightSelection.value && targetDishId > 0) {
      const bindings = form.value.optionEnabled === 1 ? buildBindingPayload() : [];
      await request.put(`/api/merchant/dishes/${targetDishId}/option-bindings`, bindings);
    }

    showSuccessToast('保存成功');
    showEdit.value = false;
    resetBindingDraft();
    await fetchDishes();
  } catch (e) {
    console.error(e);
  }
};

const startSorting = () => {
  if (filterCatId.value === 0) {
    showFailToast('请先选择需要排序的分类');
    return;
  }
  filterStatus.value = 'all';
  showCategoryPopover.value = false;
  sortDraft.value = dishes.value.map(dish => ({ ...dish }));
  sorting.value = true;
};

const cancelSorting = () => {
  if (sortSaving.value) return;
  sortDraft.value = [];
  sorting.value = false;
};

const saveSorting = async () => {
  if (sortSaving.value || filterCatId.value === 0) return;
  sortSaving.value = true;
  try {
    const baseUrl = isWeightSelection.value ? '/api/merchant/weight-ingredients' : '/api/merchant/dishes';
    await request.put(`${baseUrl}/reorder`, {
      categoryId: filterCatId.value,
      orderedIds: sortDraft.value.map(item => Number(item.id))
    });
    dishes.value = sortDraft.value.map((item, index) => ({ ...item, sort: index + 1 }));
    sorting.value = false;
    sortDraft.value = [];
    showSuccessToast('顺序已保存');
    await fetchDishes();
  } catch (e) {
    console.error(e);
  } finally {
    sortSaving.value = false;
  }
};

const toggleStatus = async (dish: any) => {
  const newStatus = dish.status === 1 ? 0 : 1;
  try {
    const baseUrl = isWeightSelection.value ? '/api/merchant/weight-ingredients' : '/api/merchant/dishes';
    await request.put(`${baseUrl}/${dish.id}/status`, { status: newStatus });
    dish.status = newStatus;
    showSuccessToast(newStatus === 1 ? '已上架' : '已下架');
  } catch (e) {
    fetchDishes();
  }
};

onMounted(async () => {
  await fetchShopConfig();
  await fetchCategories();
  if (!isWeightSelection.value) {
    await fetchOptionGroups();
    resetGroupEditor();
  }
  await fetchDishes();
});
</script>

<style scoped>
.dish-mgt-container {
  --primary-color: #1a8cff;
  --bg-color: #f8fafc;
  --text-main: #2c3e50;
  
  min-height: 100%;
  background-color: var(--bg-color);
  padding-bottom: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.dish-mgt-container--sorting {
  padding-bottom: calc(92px + env(safe-area-inset-bottom));
}

/* 1. Top Bar */
.top-bar {
  background-color: #fff;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
  position: sticky;
  top: 0;
  z-index: 10;
}

.page-title {
  font-size: 21px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 12px;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filter-tabs {
  display: flex;
  flex: 1;
  min-width: 0;
  background-color: #f1f5f9;
  border-radius: 10px;
  padding: 3px;
}

.filter-tab {
  flex: 1;
  min-width: 0;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.filter-tab.active {
  background-color: #fff;
  color: var(--primary-color);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  font-weight: 600;
}

.filter-tab.disabled,
.category-trigger-btn.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.add-btn {
  background-color: var(--primary-color);
  color: white;
  border: none;
  height: 36px;
  padding: 0 8px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  box-shadow: 0 4px 10px rgba(26, 140, 255, 0.2);
  box-sizing: border-box;
  white-space: nowrap;
  min-width: 64px;
}

.add-btn:active {
  transform: scale(0.96);
}

.add-btn:disabled,
.sort-mode-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.sort-mode-btn {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #bfdbfe;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

/* Category Filter Trigger */
.category-filter {
  flex-shrink: 0;
}

.category-trigger-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  background-color: #f8fafc;
  height: 36px;
  min-width: 118px;
  padding: 0 10px;
  border-radius: 9px;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #475569;
  font-weight: 500;
  transition: all 0.2s;
  box-sizing: border-box;
}

.category-trigger-btn:active {
  background-color: #f1f5f9;
}

.trigger-icon {
  font-size: 12px;
  transition: transform 0.2s;
}

.trigger-icon.open {
  transform: rotate(180deg);
}

.popover-scroll-container {
  max-height: 300px;
  overflow-y: auto;
  padding: 4px;
  min-width: 140px;
}

.popover-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #334155;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.popover-item:active {
  background-color: #f1f5f9;
}

.popover-item.active {
  color: var(--primary-color);
  font-weight: 600;
  background-color: #eff6ff;
}

/* 2. Dish List */
.dish-list {
  padding: 16px;
}

.dish-card-wrapper {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.trigger-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-sort-card {
  min-height: 76px;
  margin-bottom: 12px;
  padding: 12px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 28px 52px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.dish-sort-card__position {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.dish-sort-card__info {
  min-width: 0;
}

.dish-sort-card__name {
  overflow: hidden;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-sort-card__meta {
  margin-top: 5px;
  font-size: 12px;
}

.dish-sort-card__meta .is-on {
  color: #047857;
}

.dish-sort-card__meta .is-off {
  color: #64748b;
}

.dish-sort-handle {
  width: 44px;
  height: 44px;
  padding: 0;
  border: none;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #475569;
  font-size: 22px;
  cursor: grab;
  touch-action: none;
}

.dish-sort-handle:active {
  cursor: grabbing;
  background: #dbeafe;
  color: #1d4ed8;
}

.dish-sort-card--ghost {
  opacity: 0.35;
  border-color: #60a5fa;
}

.dish-sort-card--chosen,
.dish-sort-card--dragging {
  border-color: #1a8cff;
  box-shadow: 0 10px 24px rgba(26, 140, 255, 0.18);
}

.sort-action-bar {
  position: fixed;
  left: 50%;
  bottom: calc(58px + env(safe-area-inset-bottom));
  z-index: 900;
  width: calc(100% - 24px);
  max-width: 560px;
  min-height: 60px;
  padding: 8px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr) 96px;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.16);
  transform: translateX(-50%);
  backdrop-filter: blur(10px);
}

.sort-action-bar__title {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-action-bar__cancel,
.sort-action-bar__save {
  height: 42px;
  padding: 0 12px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
}

.sort-action-bar__cancel {
  background: #f1f5f9;
  color: #475569;
}

.sort-action-bar__save {
  background: #1a8cff;
  color: #fff;
}

.sort-action-bar button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dish-card {
  background-color: #fff;
  padding: 12px;
  display: flex;
  gap: 12px;
}

.dish-thumb {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.status-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.status-badge.on {
  background-color: rgba(0, 184, 148, 0.9);
  color: white;
}

.status-badge.off {
  background-color: rgba(149, 165, 166, 0.9);
  color: white;
}

.dish-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.dish-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.4;
  margin-bottom: 4px;
}

.dish-cat {
  font-size: 11px;
  color: #64748b;
  background-color: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.info-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dish-price {
  color: var(--primary-color);
  font-weight: 700;
  font-size: 18px;
}

.currency {
  font-size: 12px;
  margin-right: 2px;
}

.weight-meta {
  font-size: 14px;
  color: #0f766e;
}

.slide-tip {
  font-size: 11px;
  color: #cbd5e1;
  display: flex;
  align-items: center;
  gap: 2px;
}

/* Swipe Actions */
.swipe-actions {
  height: 100%;
  display: flex;
}

.swipe-btn {
  border: none;
  height: 100%;
  padding: 0 20px;
  color: white;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 500;
}

.swipe-btn.edit { background-color: #3b82f6; }
.swipe-btn.status.off { background-color: #ef4444; } /* To turn off */
.swipe-btn.status.on { background-color: #10b981; } /* To turn on */
.swipe-btn.delete { background-color: #94a3b8; }

/* 3. Drawer Form */
.edit-drawer {
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  background-color: #fff;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

.drawer-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.drawer-close {
  font-size: 20px;
  color: #94a3b8;
  padding: 8px;
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.form-section {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 4px solid var(--primary-color);
}

.cat-select-wrapper {
  position: relative;
  width: 100%;
}

.custom-select {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-main);
  appearance: none;
  outline: none;
}

.select-arrow {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  color: #cbd5e1;
  pointer-events: none;
}

.image-upload-area {
  width: 100%;
  height: 140px;
  border: 2px dashed #e2e8f0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: #f8fafc;
  overflow: hidden;
}

.upload-placeholder {
  text-align: center;
  color: #94a3b8;
}

.upload-placeholder .van-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.url-input {
  margin-top: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  width: 80%;
  text-align: center;
}

.preview-box {
  width: 100%;
  height: 100%;
  position: relative;
}

.remove-img {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0,0,0,0.5);
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-actions {
  position: sticky;
  bottom: 0;
  background-color: #fff;
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  border-top: 1px solid #f1f5f9;
}

.btn-cancel, .btn-submit {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  font-weight: 600;
  border: none;
  font-size: 15px;
}

.btn-cancel {
  background-color: #f1f5f9;
  color: #64748b;
}

.btn-submit {
  background-color: var(--primary-color);
  color: white;
}

.weight-suffix {
  color: #64748b;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.secondary-btn {
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #1a8cff;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.price-prefix {
  color: #64748b;
  font-size: 13px;
}

.dish-option-summary {
  margin: 4px 0 8px;
  font-size: 12px;
  color: #1a8cff;
  line-height: 1.5;
}

.option-binding-section {
  margin-top: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.option-binding-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.option-binding-section__title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.option-binding-section__desc {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.inline-manage-btn {
  border: none;
  border-radius: 999px;
  background: #1a8cff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 8px 14px;
}

.option-binding-empty,
.sheet-empty {
  padding: 18px 14px;
  border-radius: 12px;
  background: #fff;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.option-binding-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.option-binding-card + .option-binding-card {
  margin-top: 10px;
}

.option-binding-card__main {
  min-width: 0;
  flex: 1;
}

.option-binding-card__name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.option-binding-card__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.option-binding-card__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.binding-required-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.binding-sort-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.binding-sort-btn,
.binding-remove-btn,
.group-card__btn,
.group-editor-secondary,
.group-editor-primary,
.group-editor-remove,
.sheet-add-btn {
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 12px;
}

.binding-sort-btn,
.group-card__btn,
.group-editor-secondary {
  background: #f1f5f9;
  color: #475569;
}

.binding-remove-btn,
.group-card__btn--danger,
.group-editor-remove {
  background: #fff1f2;
  color: #e11d48;
}

.group-editor-primary,
.sheet-add-btn {
  background: #1a8cff;
  color: #fff;
}

.binding-sort-btn:disabled {
  opacity: 0.45;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 12px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
}

.sheet-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.sheet-close {
  width: 32px;
  height: 32px;
  border-radius: 16px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.sheet-body {
  height: calc(100% - 64px);
  overflow-y: auto;
  padding: 16px 18px 24px;
  background: #f8fafc;
}

.sheet-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.group-card {
  padding: 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.group-card + .group-card,
.group-card + .group-editor-card,
.binding-select-card + .binding-select-card {
  margin-top: 12px;
}

.group-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.group-card__name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.group-card__sub {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.group-card__actions {
  display: flex;
  gap: 8px;
}

.group-card__values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.group-value-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}

.group-editor-card {
  margin-top: 14px;
  padding: 16px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #dbeafe;
}

.group-editor-card__title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.group-editor-values {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.group-editor-value-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px 72px 64px;
  gap: 8px;
  align-items: center;
}

.group-editor-input {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  padding: 0 12px;
  font-size: 13px;
  color: #1e293b;
  box-sizing: border-box;
}

.group-editor-input--price {
  text-align: right;
}

.group-editor-default {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
}

.group-editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.binding-select-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.binding-select-card.active {
  border-color: #1a8cff;
  background: #eff6ff;
}

.binding-select-card__main {
  min-width: 0;
  flex: 1;
}

.binding-select-card__name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.binding-select-card__values {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .action-buttons {
    justify-content: stretch;
  }

  .category-filter,
  .category-trigger-btn,
  .add-btn,
  .sort-mode-btn {
    flex: 1;
  }

  .category-trigger-btn,
  .add-btn,
  .sort-mode-btn {
    min-width: 0;
  }

  .sort-mode-btn {
    padding: 0 6px;
    font-size: 13px;
  }

  .group-editor-value-row {
    grid-template-columns: 1fr;
  }

  .option-binding-card {
    flex-direction: column;
    align-items: stretch;
  }

  .option-binding-card__actions {
    align-items: stretch;
  }

  .binding-sort-actions,
  .group-card__actions,
  .group-editor-footer {
    flex-wrap: wrap;
  }
}
</style>
