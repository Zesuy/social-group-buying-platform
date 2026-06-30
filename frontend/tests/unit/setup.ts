/**
 * Vitest 测试全局设置
 *
 * 注册项目中使用的 Vant 组件到 @vue/test-utils 全局，
 * 使测试中可以直接使用 <van-*> 组件。
 */

import { config } from '@vue/test-utils'
import {
  Tabbar,
  TabbarItem,
  NavBar,
  Button,
  Form,
  Field,
  CellGroup,
  Empty,
  Loading,
  Icon,
  Tag,
  Stepper,
  Swipe,
  SwipeItem,
  Checkbox,
  List,
  PullRefresh,
  Switch,
  Tabs,
  Tab,
  Search,
  Radio,
  RadioGroup,
  ActionSheet,
  Image as VanImage,
  Step,
  Steps,
  Dialog,
} from 'vant'

// 注册项目中使用的 Vant 组件
config.global.components = {
  ...config.global.components,
  VanTabbar: Tabbar,
  VanTabbarItem: TabbarItem,
  VanNavBar: NavBar,
  VanButton: Button,
  VanForm: Form,
  VanField: Field,
  VanCellGroup: CellGroup,
  VanEmpty: Empty,
  VanLoading: Loading,
  VanIcon: Icon,
  VanTag: Tag,
  VanStepper: Stepper,
  VanSwipe: Swipe,
  VanSwipeItem: SwipeItem,
  VanCheckbox: Checkbox,
  VanList: List,
  VanPullRefresh: PullRefresh,
  VanSwitch: Switch,
  VanTabs: Tabs,
  VanTab: Tab,
  VanSearch: Search,
  VanRadio: Radio,
  VanRadioGroup: RadioGroup,
  VanActionSheet: ActionSheet,
  VanImage: VanImage,
  VanStep: Step,
  VanSteps: Steps,
  VanDialog: Dialog,
}
