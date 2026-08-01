<template>
  <view class="service-agreement-container">
    <!-- 顶部导航栏 -->
    <view class="navbar">
      <view class="navbar-left" @click="goBack">
        <uni-icons type="back" size="24" color="#333" class="nav-back"></uni-icons>
      </view>
      <view class="navbar-title">用户服务协议</view>
      <view class="navbar-right"></view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content">
        <view class="header">
          <text class="title">食刻快取 用户服务协议</text>
          <text class="update-time">更新日期：2026年3月28日</text>
          <text class="effective-time">生效日期：2026年3月28日</text>
        </view>
        
        <view class="section">
          <text class="section-title">提示条款</text>
          <text class="section-text">
            欢迎您与 食刻快取（以下简称"本平台"）共同签署本《用户服务协议》（下称"本协议"）并使用本平台服务！
          </text>
          <text class="section-text">
            各服务条款前所列索引关键词仅为帮助您理解该条款表达的主旨之用，不影响或限制本协议条款的含义或解释。为维护您自身权益，建议您仔细阅读各条款具体表述。
          </text>
          <text class="section-text">
            <text class="bold">【审慎阅读】</text>您在同意本协议之前，应当认真阅读本协议。请您务必审慎阅读、充分理解各条款内容，特别是免除或者限制责任的条款、法律适用和争议解决条款。免除或者限制责任的条款将以粗体标识，您应重点阅读。如您对本协议有任何疑问，可向本平台客服咨询。
          </text>
          <text class="section-text">
            <text class="bold">【签约动作】</text>当您按照注册页面提示填写信息、阅读并同意本协议且完成全部注册程序后，即表示您已充分阅读、理解并接受本协议的全部内容，并与本平台达成一致，成为本平台"用户"。阅读本协议的过程中，如果您不同意本协议或其中任何条款约定，您应立即停止注册程序。
          </text>
        </view>
        
        <view class="section">
          <text class="section-title">一、协议范围</text>
          
          <view class="subsection">
            <text class="subsection-title">1.1 协议主体</text>
            <text class="section-text">
              本协议是您与本平台之间关于使用本平台服务所订立的协议。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">1.2 协议内容</text>
            <text class="section-text">
              本协议内容包括协议正文、本平台已经发布的或将来可能发布的各类规则、通知、公告等（以下统称"规则"）。所有规则为本协议不可分割的组成部分，与本协议正文具有同等法律效力。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">1.3 协议变更</text>
            <text class="section-text">
              本平台有权根据需要不时地制订、修改本协议及/或各类规则，并以网站公示的方式进行变更公告，无需另行单独通知您。变更后的协议和规则一经在网站公布后，立即或在公告明确的特定时间自动生效。
            </text>
            <text class="section-text">
              若您在本协议及各类规则变更后继续使用本平台服务的，即表示您已接受变更后的协议和规则；若您不同意相关变更，应当立即停止使用本平台服务。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">二、账户注册与使用</text>
          
          <view class="subsection">
            <text class="subsection-title">2.1 用户资格</text>
            <text class="section-text">
              您确认，在您完成注册程序或以其他本平台允许的方式实际使用本平台服务时，您应当是具备完全民事行为能力的自然人。若您不具备前述主体资格，则您及您的监护人应承担因此而导致的一切后果，且本平台有权注销或永久冻结您的账户，并向您及您的监护人索偿。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">2.2 注册信息</text>
            <text class="section-text">
              您在使用本平台服务时，应提供<text class="bold">真实、准确、完整</text>的个人信息（包括但不限于姓名、手机号码等），并及时更新。如有合理理由怀疑您提供的资料错误、不实、过时或不完整的，本平台有权向您发出询问或要求改正的通知，并有权直接做出删除相应资料的处理，直至中止、终止对您提供部分或全部服务。本平台对此不承担任何责任，您将承担因此产生的任何直接或间接支出。
            </text>
            <text class="section-text">
              <text class="bold">特别提醒：</text>您提供的<text class="bold">手机号码将用于订单沟通</text>，包括但不限于：
            </text>
            <view class="list">
              <text class="list-item">订单确认与状态更新</text>
              <text class="list-item">菜品缺货时的替代方案协商</text>
              <text class="list-item">取餐提醒与通知</text>
              <text class="list-item">订单异常处理（如支付失败、订单取消等）</text>
              <text class="list-item">逃单情况的核实与处理</text>
            </view>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">2.3 账户安全</text>
            <text class="section-text">
              您须自行负责对您的登录名、微信账号和密码保密，且须对您在该登录名、微信账号和密码下发生的所有活动（包括但不限于信息披露、发布信息、网上点击同意或提交各类规则协议、订购服务等）承担责任。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">三、服务内容</text>
          
          <view class="subsection">
            <text class="subsection-title">3.1 服务说明</text>
            <text class="section-text">
              本平台向您提供校园餐饮预订服务，包括但不限于：浏览商家菜单、在线点餐、订单支付、取餐提醒等服务（以下简称"本服务"）。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">3.2 服务变更</text>
            <text class="section-text">
              您理解并同意，本平台有权根据业务发展情况随时调整服务内容，并有权根据业务需要对部分或全部服务收取费用。如相关收费服务规则发生变更，本平台将进行相应通知或公告。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">四、订单规则</text>
          
          <view class="subsection">
            <text class="subsection-title">4.1 订单生成</text>
            <text class="section-text">
              当您确认订单并完成支付后，即视为您与商家之间建立了订单合同关系。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">4.2 订单取消</text>
            <text class="section-text">
              在商家确认接单前，您可以取消订单；商家确认接单后，订单不可取消。特殊情况下（如商家无法备餐），商家有权取消订单并全额退款。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">4.3 取餐责任</text>
            <text class="section-text">
              <text class="bold">您有责任在订单约定时间内取餐。</text>如因您未及时取餐导致食物浪费或变质，商家有权不予退款。对于多次未取餐的用户，本平台有权限制或暂停您的服务使用权限。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">4.4 逃单处理</text>
            <text class="section-text">
              <text class="bold">逃单定义：</text>用户下单后未支付且未取餐，或下单后恶意取消订单造成商家损失的行为。
            </text>
            <text class="section-text">
              <text class="bold">处理措施：</text>
            </text>
            <view class="list">
              <text class="list-item">首次逃单：系统记录并发出警告，商家可能通过您提供的手机号与您联系核实情况</text>
              <text class="list-item">多次逃单：本平台有权暂停或永久冻结您的账户，限制使用本平台服务</text>
              <text class="list-item">情节严重：本平台保留追究法律责任的权利</text>
            </view>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">五、用户行为规范</text>
          
          <view class="subsection">
            <text class="subsection-title">5.1 禁止行为</text>
            <text class="section-text">
              您承诺不得利用本平台服务进行任何违法或不当活动，包括但不限于：
            </text>
            <view class="list">
              <text class="list-item">发布、传送、传播、储存违反国家法律法规禁止的内容</text>
              <text class="list-item">侵害他人名誉权、肖像权、知识产权、商业秘密等合法权利</text>
              <text class="list-item">虚构事实、隐瞒真相、误导、欺骗他人</text>
              <text class="list-item">恶意下单、恶意取消订单、逃单等扰乱平台秩序的行为</text>
              <text class="list-item">利用技术手段恶意注册、刷单、套取优惠等</text>
              <text class="list-item">其他违反法律法规或本协议约定的行为</text>
            </view>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">5.2 信息真实</text>
            <text class="section-text">
              您承诺在使用本平台服务过程中提供的所有信息（包括但不限于姓名、联系方式等）真实、准确、完整，并承担因信息不实导致的一切后果。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">六、隐私保护</text>
          <text class="section-text">
            保护用户隐私是本平台的一项基本原则。本平台将按照本平台的隐私政策（详见《隐私政策》）收集、使用、储存和分享您的个人信息。
          </text>
          <text class="section-text">
            <text class="bold">特别说明：</text>您提供的手机号码将严格用于订单相关沟通，包括：
          </text>
          <view class="list">
            <text class="list-item">订单状态通知</text>
            <text class="list-item">菜品缺货时的替代方案沟通</text>
            <text class="list-item">取餐提醒</text>
            <text class="list-item">逃单情况核实</text>
            <text class="list-item">其他与订单履行相关的必要沟通</text>
          </view>
          <text class="section-text">
            本平台承诺不会将您的手机号码用于营销推广或其他与订单无关的用途。
          </text>
        </view>
        
        <view class="section">
          <text class="section-title">七、免责声明</text>
          
          <view class="subsection">
            <text class="subsection-title">7.1 不可抗力</text>
            <text class="section-text">
              因不可抗力（包括但不限于自然灾害、战争、动乱、政府行为、黑客攻击、电信部门技术管制等）导致本协议不能履行的，遭受不可抗力的一方免予承担违约责任。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">7.2 信息真实性</text>
            <text class="section-text">
              本平台作为校园餐饮预订服务平台，不对商家提供食品的质量、安全、合法性等作任何明示或暗示的担保。您与商家之间的交易纠纷由您与商家自行解决，本平台可提供必要的协助。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">7.3 服务中断</text>
            <text class="section-text">
              鉴于网络服务的特殊性，您同意本平台有权随时中断或终止部分或全部的服务。对于因服务中断或终止造成的损失，本平台不承担任何责任。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">八、协议终止</text>
          
          <view class="subsection">
            <text class="subsection-title">8.1 终止情形</text>
            <text class="section-text">
              出现以下情况时，本平台有权终止本协议：
            </text>
            <view class="list">
              <text class="list-item">您违反本协议约定，本平台依据违约条款终止本协议的</text>
              <text class="list-item">您盗用他人账户、发布违禁信息、采取不正当手段谋利等严重违规的</text>
              <text class="list-item">您提供虚假注册信息或多次逃单的</text>
              <text class="list-item">其它应当终止服务的情况</text>
            </view>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">8.2 终止后果</text>
            <text class="section-text">
              协议终止后，本平台有权永久删除您的账户信息及相关的全部资料。协议终止不影响终止前已经产生的权利义务。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">九、法律适用与争议解决</text>
          
          <view class="subsection">
            <text class="subsection-title">9.1 法律适用</text>
            <text class="section-text">
              本协议的订立、执行和解释及争议的解决均应适用中华人民共和国大陆地区法律。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">9.2 争议解决</text>
            <text class="section-text">
              因本协议引起的或与本协议有关的任何争议，双方应友好协商解决；协商不成的，任何一方均有权将争议提交至本平台运营公司所在地有管辖权的人民法院诉讼解决。
            </text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">十、其他</text>
          
          <view class="subsection">
            <text class="subsection-title">10.1 通知送达</text>
            <text class="section-text">
              本协议项下本平台对于您所有的通知均可通过网页公告、电子邮件、手机短信或常规的信件传送等方式进行；该等通知于发送之日视为已送达收件人。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">10.2 可分割性</text>
            <text class="section-text">
              如本协议中的任何条款无论因何种原因完全或部分无效或不具有执行力，本协议的其余条款仍应有效并且有约束力。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">10.3 标题解释</text>
            <text class="section-text">
              本协议各条款的标题仅为方便阅读而设，不影响条款所包含内容的含义或解释。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">10.4 完整协议</text>
            <text class="section-text">
              本协议构成您和本平台之间就使用本平台服务所达成的完整协议，取代之前任何口头或书面的约定。
            </text>
          </view>
        </view>
        
        <view class="footer">
          <text class="footer-text">感谢您阅读本用户服务协议！</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { smartNavigateBack } from '@/utils/navigation'

// 返回上一页
const goBack = () => {
  smartNavigateBack({ fallbackTab: 'pages/my/my' })
}

// 页面加载
onLoad(() => {
  console.log('用户服务协议页面加载')
})
</script>

<style lang="scss">
.service-agreement-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #fff;
  
  .navbar {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 30rpx;
    background-color: #fff;
    border-bottom: 1rpx solid #f0f0f0;
    position: sticky;
    top: 0;
    z-index: 100;
    
    .navbar-left {
      width: 80rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .nav-back {
        font-size: 36rpx;
      }
    }
    
    .navbar-title {
      flex: 1;
      text-align: center;
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }
    
    .navbar-right {
      width: 80rpx;
    }
  }
  
  .content-scroll {
    flex: 1;
    height: calc(100vh - 88rpx);
    
    .content {
      padding: 40rpx 30rpx 80rpx;
      
      .header {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-bottom: 50rpx;
        padding-bottom: 30rpx;
        border-bottom: 2rpx solid #f0f0f0;
        
        .title {
          font-size: 36rpx;
          font-weight: bold;
          color: #333;
          text-align: center;
          margin-bottom: 20rpx;
        }
        
        .update-time,
        .effective-time {
          font-size: 26rpx;
          color: #666;
          margin-bottom: 10rpx;
        }
      }
      
      .section {
        margin-bottom: 50rpx;
        
        .section-title {
          display: block;
          font-size: 32rpx;
          font-weight: 600;
          color: #333;
          margin-bottom: 20rpx;
          line-height: 1.4;
        }
        
        .subsection {
          margin: 25rpx 0;
          
          .subsection-title {
            display: block;
            font-size: 28rpx;
            font-weight: 500;
            color: #444;
            margin-bottom: 15rpx;
            line-height: 1.4;
          }
        }
        
        .section-text {
          display: block;
          font-size: 26rpx;
          color: #555;
          line-height: 1.6;
          margin-bottom: 15rpx;
          text-align: justify;
          
          .bold {
            font-weight: 600;
            color: #333;
          }
        }
        
        .list {
          margin: 15rpx 0 15rpx 30rpx;
          
          .list-item {
            display: block;
            font-size: 26rpx;
            color: #555;
            line-height: 1.6;
            margin-bottom: 12rpx;
            position: relative;
            padding-left: 20rpx;
            text-align: justify;
            
            &::before {
              content: '•';
              position: absolute;
              left: 0;
              color: #07c160;
              font-weight: bold;
            }
            
            .bold {
              font-weight: 600;
              color: #333;
            }
          }
        }
      }
      
      .footer {
        margin-top: 60rpx;
        padding-top: 40rpx;
        border-top: 1rpx solid #f0f0f0;
        text-align: center;
        
        .footer-text {
          font-size: 28rpx;
          color: #666;
          font-style: italic;
        }
      }
    }
  }
}
</style>
