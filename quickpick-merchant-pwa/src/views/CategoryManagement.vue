<template>
  <div class="category-mgt-container" :class="{ 'category-mgt-container--sorting': sorting }">
    <div class="top-bar">
      <div class="page-title">{{ isWeightSelection ? '分类与基础配置' : '分类管理' }}</div>
      <div class="top-actions">
        <button class="sort-btn" :disabled="sorting" @click="startSorting">
          <van-icon name="sort" /> 调整顺序
        </button>
        <button
          v-if="!isWeightSelection"
          class="option-btn"
          :disabled="sorting"
          @click="openOptionGroupManager"
        >
          <van-icon name="setting-o" /> 规格模板
        </button>
        <button class="add-btn" :disabled="sorting" @click="onAdd">
          <van-icon name="plus" /> 新建分类
        </button>
      </div>
    </div>

    <div v-if="isWeightSelection" class="config-wrapper">
      <div class="config-card">
        <div class="config-card__header">
          <div>
            <div class="config-title">称重配置</div>
            <div class="config-desc">这三项会直接影响前台预计价格、最低可下单重量和店铺模式。</div>
          </div>
          <button class="config-action-btn" @click="openConfigDialog">编辑称重配置</button>
        </div>

        <div class="config-grid">
          <div class="config-item">
            <span class="config-item__label">店铺模式</span>
            <span class="config-item__value">{{ shopConfig.shopMode === 'weight_selection' ? '自选称重店' : '固定菜品店' }}</span>
          </div>
          <div class="config-item">
            <span class="config-item__label">每500g价格</span>
            <span class="config-item__value">¥{{ shopConfig.weightPricePer500g || '--' }}</span>
          </div>
          <div class="config-item">
            <span class="config-item__label">最低下单重量</span>
            <span class="config-item__value">{{ shopConfig.minimumOrderWeightG || 0 }}g</span>
          </div>
        </div>
      </div>

      <div class="config-card broth-card">
        <div class="config-card__header">
          <div>
            <div class="config-title">汤底管理</div>
            <div class="config-desc">汤底属于低频维护项，不单独占底部导航，统一在这里集中管理。</div>
          </div>
          <button class="config-action-btn secondary" @click="showBrothPopup = true">管理汤底</button>
        </div>

        <div v-if="brothOptions.length === 0" class="empty-inline">暂未配置汤底</div>
        <div v-else class="broth-list">
          <div v-for="item in brothOptions" :key="item.id" class="broth-chip">
            <img v-if="item.image" :src="item.image" class="broth-chip__image" alt="汤底图片" />
            <span>{{ item.name }}</span>
            <span class="broth-chip__price" v-if="Number(item.extraPrice || 0) > 0">+{{ item.extraPrice }}元</span>
          </div>
        </div>
      </div>
    </div>

    <div class="category-list">
      <div v-if="categories.length === 0" class="empty-state">
        <van-empty image="default" :description="emptyDescription" />
      </div>

      <div v-if="!sorting" class="list-wrapper">
        <van-swipe-cell v-for="cat in categories" :key="cat.id" class="cat-card-wrapper">
          <div class="cat-card">
            <div class="category-icon">
              <van-icon name="label-o" />
            </div>

            <div class="cat-info">
              <div class="cat-name">{{ cat.name }}</div>
            </div>

            <div class="slide-tip">
              <van-icon name="arrow-left" /> 左滑管理
            </div>
          </div>

          <template #right>
            <div class="swipe-actions">
              <button class="swipe-btn edit" @click="onEdit(cat)">
                <van-icon name="edit" />
                <span>编辑</span>
              </button>
              <button class="swipe-btn delete" @click="onDelete(cat)">
                <van-icon name="delete-o" />
                <span>删除</span>
              </button>
            </div>
          </template>
        </van-swipe-cell>
      </div>

      <draggable
        v-else
        v-model="sortDraft"
        item-key="id"
        handle=".category-sort-handle"
        :animation="180"
        ghost-class="category-sort-card--ghost"
        chosen-class="category-sort-card--chosen"
        drag-class="category-sort-card--dragging"
      >
        <template #item="{ element: cat, index }">
          <div class="category-sort-card">
            <div class="category-sort-card__position">{{ index + 1 }}</div>
            <div class="category-sort-card__name">{{ cat.name }}</div>
            <button type="button" class="category-sort-handle" :aria-label="`拖动${cat.name}`">
              <van-icon name="wap-nav" />
            </button>
          </div>
        </template>
      </draggable>
    </div>

    <div v-if="sorting" class="category-sort-actions">
      <button type="button" class="category-sort-actions__cancel" :disabled="sortSaving" @click="cancelSorting">取消</button>
      <div class="category-sort-actions__title">分类顺序</div>
      <button type="button" class="category-sort-actions__save" :disabled="sortSaving" @click="saveSorting">
        {{ sortSaving ? '保存中...' : '保存顺序' }}
      </button>
    </div>

    <van-dialog
      v-model:show="showAdd"
      :title="editingId ? '编辑分类' : '新建分类'"
      show-cancel-button
      @confirm="onSave"
      class="custom-dialog"
    >
      <div class="dialog-content">
        <div class="input-group">
          <label>分类名称</label>
          <input
            v-model="form.name"
            type="text"
            :placeholder="isWeightSelection ? '例如：丸子类 / 蔬菜类' : '例如：招牌热销'"
            class="modern-input"
          />
        </div>

      </div>
    </van-dialog>

    <van-dialog
      v-model:show="showConfigDialog"
      title="编辑称重配置"
      show-cancel-button
      @confirm="saveWeightConfig"
      class="custom-dialog"
    >
      <div class="dialog-content">
        <div class="input-group">
          <label>店铺模式</label>
          <select v-model="weightForm.shopMode" class="modern-input">
            <option value="fixed_dish">固定菜品店</option>
            <option value="weight_selection">自选称重店</option>
          </select>
        </div>
        <div v-if="weightForm.shopMode === 'weight_selection'" class="input-group">
          <label>每500g价格</label>
          <input v-model="weightForm.weightPricePer500g" type="number" min="0" step="0.01" class="modern-input" placeholder="例如 18.00" />
        </div>
        <div v-if="weightForm.shopMode === 'weight_selection'" class="input-group">
          <label>最低下单重量(g)</label>
          <input v-model="weightForm.minimumOrderWeightG" type="number" min="0" step="1" class="modern-input" placeholder="例如 250" />
        </div>
        <div class="helper-text">
          每500g价格会影响前台预计价格，最低下单重量会影响用户能否提交自选称重订单。
        </div>
      </div>
    </van-dialog>

    <van-popup v-model:show="showBrothPopup" position="bottom" round :style="{ minHeight: '72vh' }">
      <div class="popup-shell">
        <div class="popup-header">
          <div class="popup-title">汤底管理</div>
          <div class="popup-close" @click="closeBrothPopup">
            <van-icon name="cross" />
          </div>
        </div>

        <div class="popup-body">
          <div class="input-group">
            <label>汤底名称</label>
            <input v-model="brothForm.name" type="text" class="modern-input" placeholder="例如：经典骨汤" />
          </div>
          <div class="input-group">
            <label>附加价</label>
            <input v-model="brothForm.extraPrice" type="number" min="0" step="0.01" class="modern-input" placeholder="例如：1.00" />
          </div>
          <div class="input-group">
            <label>汤底图片</label>
            <van-uploader
              v-model="brothFileList"
              :max-count="1"
              :after-read="afterReadBrothImage"
              :max-size="5 * 1024 * 1024"
              @oversize="onBrothImageOversize"
            />
            <div class="helper-text">建议上传 3:2 或 1:1 图片，前台会优先展示真实汤底图片。</div>
          </div>
          <div class="input-group">
            <label>排序权重</label>
            <input v-model="brothForm.sort" type="number" min="0" step="1" class="modern-input" placeholder="数字越小越靠前" />
          </div>

          <button class="popup-primary-btn" @click="saveBroth">{{ brothEditingId ? '保存汤底' : '新增汤底' }}</button>

          <div class="broth-manage-list">
            <div v-if="brothOptions.length === 0" class="empty-inline">暂无汤底，先创建一个吧</div>
            <div v-for="item in brothOptions" :key="item.id" class="broth-row">
              <img v-if="item.image" :src="item.image" class="broth-row__image" alt="汤底图片" />
              <div class="broth-row__main">
                <div class="broth-row__name">{{ item.name }}</div>
                <div class="broth-row__meta">附加价：{{ item.extraPrice }} 元 · 排序：{{ item.sort }}</div>
              </div>
              <div class="broth-row__actions">
                <button class="small-outline-btn" @click="startEditBroth(item)">编辑</button>
                <button class="small-outline-btn danger" @click="removeBroth(item.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
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
          <div class="input-group">
            <label>规格组名称</label>
            <input v-model.trim="groupForm.name" class="modern-input" placeholder="如：份量、辣度" />
          </div>

          <div class="group-editor-values">
            <div v-for="(value, index) in groupForm.values" :key="value.localKey" class="group-editor-value-row">
              <input v-model.trim="value.name" class="modern-input" placeholder="规格值名称" />
              <input v-model="value.extraPrice" class="modern-input" type="number" placeholder="加价" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import draggable from 'vuedraggable';
import request from '@/utils/request';
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';

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

const categories = ref<any[]>([]);
const sorting = ref(false);
const sortSaving = ref(false);
const sortDraft = ref<any[]>([]);
const showAdd = ref(false);
const editingId = ref<number | null>(null);
const showConfigDialog = ref(false);
const showBrothPopup = ref(false);
const brothOptions = ref<any[]>([]);
const brothEditingId = ref<number | null>(null);
const brothFileList = ref<any[]>([]);
const showOptionGroupManager = ref(false);
const optionGroups = ref<OptionGroupItem[]>([]);
const groupEditorVisible = ref(false);
const editingGroupId = ref<number | null>(null);
const groupForm = ref<{
  name: string;
  values: OptionValueDraft[];
}>({
  name: '',
  values: []
});

const form = ref({ name: '' });
const shopConfig = ref<any>({
  shopMode: 'fixed_dish',
  weightPricePer500g: null,
  minimumOrderWeightG: 0
});
const weightForm = reactive({
  shopMode: 'fixed_dish',
  weightPricePer500g: '',
  minimumOrderWeightG: 0
});
const brothForm = reactive({
  name: '',
  image: '',
  extraPrice: '0',
  sort: '0',
  status: 1
});

const isWeightSelection = computed(() => shopConfig.value.shopMode === 'weight_selection');
const emptyDescription = computed(() =>
  isWeightSelection.value
    ? '暂无食材分类，点击上方“新建”创建分类后再去管理食材。'
    : '暂无分类，点击上方“新建”创建您的第一个分类吧！'
);

const createLocalKey = () => `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

const createGroupValueDraft = (value?: Partial<OptionValueDraft>): OptionValueDraft => ({
  localKey: createLocalKey(),
  id: value?.id,
  name: value?.name || '',
  extraPrice: value?.extraPrice ?? '0',
  isDefault: value?.isDefault === 1 ? 1 : 0,
  sort: value?.sort ?? 0
});

const resetGroupEditor = () => {
  editingGroupId.value = null;
  groupEditorVisible.value = false;
  groupForm.value = {
    name: '',
    values: [createGroupValueDraft({ isDefault: 1, extraPrice: '0' })]
  };
};

const fetchCategories = async () => {
  try {
    const res: any = await request.get('/api/merchant/categories');
    categories.value = res;
  } catch (e) {
    console.error(e);
  }
};

const fetchOptionGroups = async () => {
  if (isWeightSelection.value) {
    optionGroups.value = [];
    return;
  }
  try {
    const res: any = await request.get('/api/merchant/dish-option-groups');
    optionGroups.value = Array.isArray(res) ? res : [];
  } catch (e) {
    console.error(e);
  }
};

const fetchShopConfig = async () => {
  try {
    const res: any = await request.get('/api/merchant/shop');
    shopConfig.value = {
      shopMode: res.shopMode || 'fixed_dish',
      weightPricePer500g: res.weightPricePer500g,
      minimumOrderWeightG: res.minimumOrderWeightG || 0
    };
  } catch (e) {
    console.error(e);
  }
};

const fetchBrothOptions = async () => {
  if (!isWeightSelection.value) {
    brothOptions.value = [];
    return;
  }
  try {
    const res: any = await request.get('/api/merchant/broth-options');
    brothOptions.value = res;
  } catch (e) {
    console.error(e);
  }
};

const resetForm = () => {
  editingId.value = null;
  form.value = { name: '' };
};

const onAdd = () => {
  if (sorting.value) return;
  resetForm();
  showAdd.value = true;
};

const onEdit = (cat: any) => {
  if (sorting.value) return;
  editingId.value = cat.id;
  form.value = { name: cat.name };
  showAdd.value = true;
};

const onSave = async () => {
  if (!form.value.name.trim()) {
    showFailToast('请输入分类名称');
    return;
  }
  try {
    const payload = { name: form.value.name.trim() };
    if (editingId.value) {
      await request.put(`/api/merchant/categories/${editingId.value}`, payload);
    } else {
      await request.post('/api/merchant/categories', payload);
    }
    showSuccessToast('保存成功');
    fetchCategories();
    resetForm();
  } catch (e) {
    console.error(e);
  }
};

const startSorting = () => {
  if (categories.value.length === 0) {
    showFailToast('暂无可排序的分类');
    return;
  }
  sortDraft.value = categories.value.map(category => ({ ...category }));
  sorting.value = true;
};

const cancelSorting = () => {
  if (sortSaving.value) return;
  sortDraft.value = [];
  sorting.value = false;
};

const saveSorting = async () => {
  if (sortSaving.value) return;
  sortSaving.value = true;
  try {
    await request.put('/api/merchant/categories/reorder', {
      orderedIds: sortDraft.value.map(category => Number(category.id))
    });
    categories.value = sortDraft.value.map((category, index) => ({ ...category, sort: index + 1 }));
    sortDraft.value = [];
    sorting.value = false;
    showSuccessToast('分类顺序已保存');
    await fetchCategories();
  } catch (e) {
    console.error(e);
  } finally {
    sortSaving.value = false;
  }
};

const onDelete = async (cat: any) => {
  showConfirmDialog({
    title: '确定删除吗？',
    message: `删除“${cat.name}”分类后，其下的${isWeightSelection.value ? '食材' : '菜品'}将变为未分类状态。此操作不可撤销。`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ef4444',
    cancelButtonText: '再想想'
  }).then(async () => {
    try {
      await request.delete(`/api/merchant/categories/${cat.id}`);
      showSuccessToast('删除成功');
      fetchCategories();
    } catch (err) {}
  }).catch(() => {});
};

const openConfigDialog = () => {
  weightForm.shopMode = shopConfig.value.shopMode || 'fixed_dish';
  weightForm.weightPricePer500g = shopConfig.value.weightPricePer500g ?? '';
  weightForm.minimumOrderWeightG = shopConfig.value.minimumOrderWeightG ?? 0;
  showConfigDialog.value = true;
};

const saveWeightConfig = async () => {
  try {
    await request.put('/api/merchant/shop/weight-config', {
      shopMode: weightForm.shopMode,
      weightPricePer500g: weightForm.shopMode === 'weight_selection' ? Number(weightForm.weightPricePer500g) : null,
      minimumOrderWeightG: weightForm.shopMode === 'weight_selection' ? Number(weightForm.minimumOrderWeightG) : 0
    });
    showSuccessToast('保存成功');
    await fetchShopConfig();
    await fetchBrothOptions();
  } catch (error: any) {
    showFailToast(error?.message || '保存失败');
  }
};

const resetBrothForm = () => {
  brothEditingId.value = null;
  brothForm.name = '';
  brothForm.image = '';
  brothForm.extraPrice = '0';
  brothForm.sort = '0';
  brothFileList.value = [];
};

const startEditBroth = (item: any) => {
  brothEditingId.value = item.id;
  brothForm.name = item.name;
  brothForm.image = item.image || '';
  brothForm.extraPrice = String(item.extraPrice ?? 0);
  brothForm.sort = String(item.sort ?? 0);
  brothFileList.value = item.image ? [{ url: item.image, isImage: true }] : [];
};

const afterReadBrothImage = async (file: any) => {
  file.status = 'uploading';
  file.message = '上传中...';

  const formData = new FormData();
  formData.append('file', file.file);

  try {
    const res: any = await request.post('/api/merchant/upload/broth-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });

    brothForm.image = res.url;
    file.status = 'done';
    file.message = '上传成功';
    file.url = res.url;
  } catch (error: any) {
    file.status = 'failed';
    file.message = '上传失败';
    brothFileList.value = [];
    showFailToast(error?.message || '图片上传失败');
  }
};

const onBrothImageOversize = () => {
  showFailToast('文件大小不能超过 5MB');
};

const saveBroth = async () => {
  const payload = {
    name: brothForm.name.trim(),
    image: brothForm.image || null,
    extraPrice: Number(brothForm.extraPrice || 0),
    sort: Number(brothForm.sort || 0),
    status: 1
  };
  if (!payload.name) {
    showFailToast('请输入汤底名称');
    return;
  }
  try {
    if (brothEditingId.value) {
      await request.put(`/api/merchant/broth-options/${brothEditingId.value}`, payload);
    } else {
      await request.post('/api/merchant/broth-options', payload);
    }
    showSuccessToast('保存成功');
    resetBrothForm();
    fetchBrothOptions();
  } catch (error: any) {
    showFailToast(error?.message || '保存失败');
  }
};

const removeBroth = async (id: number) => {
  showConfirmDialog({ title: '删除汤底', message: '确认删除该汤底吗？' }).then(async () => {
    await request.delete(`/api/merchant/broth-options/${id}`);
    showSuccessToast('删除成功');
    fetchBrothOptions();
  });
};

const closeBrothPopup = () => {
  showBrothPopup.value = false;
  resetBrothForm();
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
  } catch (e) {
    console.error(e);
  }
};

onMounted(async () => {
  await fetchShopConfig();
  await fetchCategories();
  await fetchBrothOptions();
  if (!isWeightSelection.value) {
    await fetchOptionGroups();
    resetGroupEditor();
  }
});
</script>

<style scoped>
.category-mgt-container {
  --primary-color: #1a8cff;
  --bg-color: #f8fafc;
  --text-main: #2c3e50;
  min-height: 100%;
  background-color: var(--bg-color);
  padding-bottom: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.category-mgt-container--sorting {
  padding-bottom: calc(92px + env(safe-area-inset-bottom));
}

.top-bar {
  background-color: #fff;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-btn,
.add-btn,
.sort-btn {
  background-color: #f1f5f9;
  color: var(--primary-color);
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-btn {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
}

.option-btn:disabled,
.add-btn:disabled,
.sort-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.option-btn {
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.config-wrapper,
.category-list {
  padding: 16px;
}

.config-wrapper {
  padding-bottom: 0;
}

.config-card {
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid #d9ebff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(42, 139, 255, 0.08);
}

.broth-card {
  background: linear-gradient(135deg, #fffaf5 0%, #fff3e8 100%);
  border-color: #ffe0bf;
  box-shadow: 0 4px 16px rgba(255, 138, 61, 0.08);
}

.config-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.config-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.config-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.config-action-btn {
  border: none;
  background: #2a8bff;
  color: #fff;
  border-radius: 8px;
  padding: 9px 14px;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.config-action-btn.secondary {
  background: #ff8a3d;
}

.config-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.config-item {
  background: rgba(255,255,255,0.86);
  border-radius: 12px;
  padding: 12px;
}

.config-item__label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.config-item__value {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.empty-inline {
  margin-top: 14px;
  color: #94a3b8;
  font-size: 13px;
}

.broth-list {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.broth-chip {
  display: flex;
  gap: 8px;
  align-items: center;
  background: rgba(255,255,255,0.86);
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 13px;
  color: #475569;
}

.broth-chip__image {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.broth-chip__price {
  color: #ff8a3d;
  font-weight: 600;
}

.cat-card-wrapper {
  margin-bottom: 12px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.cat-card {
  background-color: #fff;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.category-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #1a8cff;
  font-size: 19px;
}

.cat-info {
  flex: 1;
}

.cat-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.cat-meta {
  font-size: 12px;
  color: #94a3b8;
}

.category-sort-card {
  min-height: 64px;
  margin-bottom: 12px;
  padding: 10px 12px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.category-sort-card__position {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.category-sort-card__name {
  min-width: 0;
  overflow: hidden;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-sort-handle {
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

.category-sort-handle:active {
  background: #dbeafe;
  color: #1d4ed8;
  cursor: grabbing;
}

.category-sort-card--ghost {
  opacity: 0.35;
  border-color: #60a5fa;
}

.category-sort-card--chosen,
.category-sort-card--dragging {
  border-color: #1a8cff;
  box-shadow: 0 10px 24px rgba(26, 140, 255, 0.18);
}

.category-sort-actions {
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

.category-sort-actions__title {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.category-sort-actions__cancel,
.category-sort-actions__save {
  height: 42px;
  padding: 0 12px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
}

.category-sort-actions__cancel {
  background: #f1f5f9;
  color: #475569;
}

.category-sort-actions__save {
  background: #1a8cff;
  color: #fff;
}

.category-sort-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.slide-tip {
  font-size: 11px;
  color: #cbd5e1;
  display: flex;
  align-items: center;
  gap: 2px;
}

.swipe-actions {
  height: 100%;
  display: flex;
}

.swipe-btn {
  border: none;
  height: 100%;
  padding: 0 24px;
  color: white;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 500;
}

.swipe-btn.edit { background-color: #3b82f6; }
.swipe-btn.delete { background-color: #ef4444; }

.custom-dialog {
  border-radius: 16px;
}

.dialog-content {
  padding: 20px 24px;
}

.input-group {
  margin-bottom: 16px;
}

.input-group label {
  display: block;
  font-size: 14px;
  color: var(--text-main);
  margin-bottom: 8px;
  font-weight: 500;
}

.modern-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  background: #fff;
}

.modern-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(26, 140, 255, 0.1);
}

.helper-text {
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px;
}

.popup-shell {
  padding: 18px 16px 24px;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.popup-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.popup-close {
  width: 32px;
  height: 32px;
  border-radius: 16px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.popup-body {
  padding-bottom: env(safe-area-inset-bottom);
}

.popup-primary-btn {
  width: 100%;
  border: none;
  border-radius: 10px;
  padding: 12px;
  background: #2a8bff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.broth-manage-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.broth-row {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.broth-row__image {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}

.broth-row__main {
  flex: 1;
}

.broth-row__name {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.broth-row__meta {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.broth-row__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.small-outline-btn {
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
}

.small-outline-btn.danger {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.empty-state {
  padding: 40px 0;
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

.sheet-add-btn,
.group-card__btn,
.group-editor-secondary,
.group-editor-primary,
.group-editor-remove {
  border: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  padding: 7px 12px;
}

.sheet-add-btn,
.group-editor-primary {
  background: #1a8cff;
  color: #fff;
}

.group-card__btn,
.group-editor-secondary {
  background: #f1f5f9;
  color: #475569;
}

.group-card__btn--danger,
.group-editor-remove {
  background: #fff1f2;
  color: #e11d48;
}

.sheet-empty {
  padding: 18px 14px;
  border-radius: 12px;
  background: #fff;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.group-card {
  padding: 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.group-card + .group-card,
.group-card + .group-editor-card {
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

@media (max-width: 768px) {
  .top-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .top-actions {
    width: 100%;
    gap: 6px;
    flex-wrap: nowrap;
  }

  .top-actions > button {
    flex: 1 1 0;
    min-width: 0;
    justify-content: center;
    padding: 8px 6px;
    font-size: 13px;
    white-space: nowrap;
  }

  .config-card__header,
  .group-card__header {
    flex-direction: column;
  }

  .config-grid,
  .group-editor-value-row {
    grid-template-columns: 1fr;
  }

  .group-card__actions,
  .group-editor-footer {
    flex-wrap: wrap;
  }
}
</style>
