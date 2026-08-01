<template>
  <view class="privacy-policy-container">
    <!-- 顶部导航栏 -->
    <view class="navbar">
      <view class="navbar-left" @click="goBack">
        <uni-icons type="back" size="24" color="#333" class="nav-back"></uni-icons>
      </view>
      <view class="navbar-title">隐私政策</view>
      <view class="navbar-right"></view>
    </view>
    
    <!-- 内容区域 -->
    <scroll-view class="content-scroll" scroll-y>
      <view class="content">
        <view class="header">
          <text class="title">食刻快取 隐私政策</text>
          <text class="update-time">更新日期：2026年3月28日</text>
          <text class="effective-time">生效日期：2026年3月28日</text>
        </view>
        
        <view class="section">
          <text class="section-title">引言</text>
          <text class="section-text">
            欢迎使用 食刻快取（以下简称"本服务"）。我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。我们致力于维持您对我们的信任，恪守以下原则，保护您的个人信息：权责一致原则、目的明确原则、选择同意原则、最少够用原则、确保安全原则、主体参与原则、公开透明原则。
          </text>
          <text class="section-text">
            本《隐私政策》将帮助您了解以下内容：
          </text>
          <view class="list">
            <text class="list-item">1. 我们如何收集和使用您的个人信息</text>
            <text class="list-item">2. 我们如何使用 Cookie 和同类技术</text>
            <text class="list-item">3. 我们如何共享、转让、公开披露您的个人信息</text>
            <text class="list-item">4. 我们如何保护您的个人信息</text>
            <text class="list-item">5. 您的权利</text>
            <text class="list-item">6. 我们如何处理未成年人的个人信息</text>
            <text class="list-item">7. 您的个人信息如何在全球范围转移</text>
            <text class="list-item">8. 本政策如何更新</text>
            <text class="list-item">9. 如何联系我们</text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">1. 我们如何收集和使用您的个人信息</text>
          <text class="section-text">
            个人信息是指以电子或者其他方式记录的能够单独或者与其他信息结合识别特定自然人身份或者反映特定自然人活动情况的各种信息。
          </text>
          
          <view class="subsection">
            <text class="subsection-title">1.1 我们收集的个人信息</text>
            <text class="section-text">
              在您使用本服务的过程中，我们会收集以下必要信息：
            </text>
            <view class="list">
              <text class="list-item"><text class="bold">姓名</text>：用于店铺人员识别您的身份，确保订单准确送达</text>
              <text class="list-item"><text class="bold">手机号码</text>：通过微信一键登录获取，用于店铺人员与您沟通订单相关事宜</text>
              <text class="list-item"><text class="bold">微信 OpenID</text>：用于识别您的微信账号，实现一键登录功能</text>
            </view>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">1.2 我们如何使用您的个人信息</text>
            <text class="section-text">
              我们会在以下情况下使用您的个人信息：
            </text>
            <view class="list">
              <text class="list-item"><text class="bold">订单沟通</text>：当您的订单出现问题时（如菜品缺货、订单异常），店铺人员会使用您提供的手机号与您联系，协商解决方案</text>
              <text class="list-item"><text class="bold">取餐提醒</text>：当您下单后未及时取餐时，店铺人员可能会通过手机号提醒您取餐，避免食物浪费</text>
              <text class="list-item"><text class="bold">逃单处理</text>：如果您下单后未支付且未取餐（逃单），店铺人员会通过手机号与您核实情况，维护店铺正常运营秩序</text>
              <text class="list-item"><text class="bold">服务改进</text>：基于匿名化的数据统计，改进我们的服务质量</text>
              <text class="list-item"><text class="bold">安全保障</text>：用于身份验证、安全防范、诈骗监测、存档备份等</text>
            </view>
            <text class="section-text">
              我们承诺，您的手机号码仅用于与订单相关的必要沟通，不会用于营销推广或其他与订单无关的用途。
            </text>
          </view>
          
          <view class="subsection">
            <text class="subsection-title">1.3 征得授权同意的例外</text>
            <text class="section-text">
              根据相关法律法规规定，以下情形中收集您的个人信息无需征得您的授权同意：
            </text>
            <view class="list">
              <text class="list-item">与国家安全、国防安全有关的</text>
              <text class="list-item">与公共安全、公共卫生、重大公共利益有关的</text>
              <text class="list-item">与犯罪侦查、起诉、审判和判决执行等有关的</text>
              <text class="list-item">出于维护个人信息主体或其他个人的生命、财产等重大合法权益但又很难得到您本人同意的</text>
              <text class="list-item">所收集的个人信息是您自行向社会公众公开的</text>
              <text class="list-item">从合法公开披露的信息中收集个人信息的，如合法的新闻报道、政府信息公开等渠道</text>
              <text class="list-item">根据您的要求签订合同所必需的</text>
              <text class="list-item">用于维护所提供的产品或服务的安全稳定运行所必需的，例如发现、处置产品或服务的故障</text>
              <text class="list-item">法律法规规定的其他情形</text>
            </view>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">2. 我们如何使用 Cookie 和同类技术</text>
          <text class="section-text">
            为确保网站正常运转，我们会在您的计算机或移动设备上存储名为 Cookie 的小数据文件。Cookie 通常包含标识符、站点名称以及一些号码和字符。借助于 Cookie，网站能够存储您的偏好等数据。
          </text>
          <text class="section-text">
            我们不会将 Cookie 用于本政策所述目的之外的任何用途。您可根据自己的偏好管理或删除 Cookie。有关详情，请参见 AboutCookies.org。您可以清除计算机上保存的所有 Cookie，大部分网络浏览器都设有阻止 Cookie 的功能。
          </text>
        </view>
        
        <view class="section">
          <text class="section-title">3. 我们如何共享、转让、公开披露您的个人信息</text>
          <text class="section-text">
            <text class="bold">共享</text>：我们不会与任何公司、组织和个人分享您的个人信息，但以下情况除外：
          </text>
          <view class="list">
            <text class="list-item">在获取明确同意的情况下共享：获得您的明确同意后，我们会与其他方共享您的个人信息</text>
            <text class="list-item">我们可能会根据法律法规规定，或按政府主管部门的强制性要求，对外共享您的个人信息</text>
            <text class="list-item">与授权合作伙伴共享：仅为实现本政策中声明的目的，我们的某些服务将由授权合作伙伴提供。我们可能会与合作伙伴共享您的某些个人信息，以提供更好的客户服务和用户体验。我们只会共享必要的个人信息，且受本隐私政策中所声明目的的约束</text>
          </view>
          
          <text class="section-text">
            <text class="bold">转让</text>：我们不会将您的个人信息转让给任何公司、组织和个人，但以下情况除外：
          </text>
          <view class="list">
            <text class="list-item">在获取明确同意的情况下转让：获得您的明确同意后，我们会向其他方转让您的个人信息</text>
            <text class="list-item">在涉及合并、收购或破产清算时，如涉及到个人信息转让，我们会在要求新的持有您个人信息的公司、组织继续受此隐私政策的约束，否则我们将要求该公司、组织重新向您征求授权同意</text>
          </view>
          
          <text class="section-text">
            <text class="bold">公开披露</text>：我们仅会在以下情况下，公开披露您的个人信息：
          </text>
          <view class="list">
            <text class="list-item">获得您明确同意后</text>
            <text class="list-item">基于法律的披露：在法律、法律程序、诉讼或政府主管部门强制性要求的情况下，我们可能会公开披露您的个人信息</text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">4. 我们如何保护您的个人信息</text>
          <text class="section-text">
            我们已使用符合业界标准的安全防护措施保护您提供的个人信息，防止数据遭到未经授权访问、公开披露、使用、修改、损坏或丢失。我们会采取一切合理可行的措施，保护您的个人信息。例如：
          </text>
          <view class="list">
            <text class="list-item">我们会对员工进行数据安全培训，加强员工对于保护个人信息重要性的认识</text>
            <text class="list-item">我们严格限制访问信息的人员范围，要求他们遵守保密义务，并进行审计</text>
            <text class="list-item">我们会使用加密技术确保数据的保密性</text>
            <text class="list-item">我们会使用受信赖的保护机制防止数据遭到恶意攻击</text>
            <text class="list-item">我们会部署访问控制机制，确保只有授权人员才可访问个人信息</text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">5. 您的权利</text>
          <text class="section-text">
            按照中国相关的法律、法规、标准，以及其他国家、地区的通行做法，我们保障您对自己的个人信息行使以下权利：
          </text>
          <view class="list">
            <text class="list-item"><text class="bold">访问您的个人信息</text>：您有权访问您的个人信息，法律法规规定的例外情况除外</text>
            <text class="list-item"><text class="bold">更正您的个人信息</text>：当您发现我们处理的关于您的个人信息有错误时，您有权要求我们做出更正</text>
            <text class="list-item"><text class="bold">删除您的个人信息</text>：在以下情形中，您可以向我们提出删除个人信息的请求：
              <view class="sublist">
                <text class="sublist-item">如果我们处理个人信息的行为违反法律法规</text>
                <text class="sublist-item">如果我们收集、使用您的个人信息，却未征得您的同意</text>
                <text class="sublist-item">如果我们处理个人信息的行为违反了与您的约定</text>
                <text class="sublist-item">如果您不再使用我们的产品或服务</text>
                <text class="sublist-item">如果我们不再为您提供产品或服务</text>
              </view>
            </text>
            <text class="list-item"><text class="bold">撤回同意</text>：您可以通过注销账号的方式，撤回此前就个人信息处理对我们做出的同意授权</text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">6. 我们如何处理未成年人的个人信息</text>
          <text class="section-text">
            我们的产品、网站和服务主要面向成人。如果没有父母或监护人的同意，未成年人不得创建自己的用户账户。
          </text>
          <text class="section-text">
            对于经父母同意而收集未成年人个人信息的情况，我们只会在受到法律允许、父母或监护人明确同意或者保护未成年人所必要的情况下使用或公开披露此信息。
          </text>
        </view>
        
        <view class="section">
          <text class="section-title">7. 您的个人信息如何在全球范围转移</text>
          <text class="section-text">
            原则上，我们在中华人民共和国境内收集和产生的个人信息，将存储在中华人民共和国境内。
          </text>
        </view>
        
        <view class="section">
          <text class="section-title">8. 本政策如何更新</text>
          <text class="section-text">
            我们的隐私政策可能变更。未经您明确同意，我们不会削减您按照本隐私政策所应享有的权利。我们会在本页面上发布对隐私政策所做的任何变更。
          </text>
          <text class="section-text">
            对于重大变更，我们还会提供更为显著的通知（包括对于某些服务，我们会通过电子邮件发送通知，说明隐私政策的具体变更内容）。
          </text>
          <text class="section-text">
            本政策所指的重大变更包括但不限于：
          </text>
          <view class="list">
            <text class="list-item">我们的服务模式发生重大变化。如处理个人信息的目的、处理的个人信息类型、个人信息的使用方式等</text>
            <text class="list-item">我们在所有权结构、组织架构等方面发生重大变化。如业务调整、破产并购等引起的所有者变更等</text>
            <text class="list-item">个人信息共享、转让或公开披露的主要对象发生变化</text>
            <text class="list-item">您参与个人信息处理方面的权利及其行使方式发生重大变化</text>
            <text class="list-item">我们负责处理个人信息安全的责任部门、联络方式及投诉渠道发生变化时</text>
            <text class="list-item">个人信息安全影响评估报告表明存在高风险时</text>
          </view>
        </view>
        
        <view class="section">
          <text class="section-title">9. 如何联系我们</text>
          <text class="section-text">
            如果您对本隐私政策有任何疑问、意见或建议，通过以下方式与我们联系：
          </text>
          <view class="list">
            <text class="list-item">邮箱：contact@example.com</text>
            <text class="list-item">电话：15979298559</text>
            <text class="list-item">地址：南昌市青山湖区湖坊镇洪都村洪都中大道169支路6号A幢3单元404室</text>
          </view>
          <text class="section-text">
            一般情况下，我们将在三十天内回复。
          </text>
        </view>
        
        <view class="footer">
          <text class="footer-text">感谢您花时间了解我们的隐私政策！</text>
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
  console.log('隐私政策页面加载')
})
</script>

<style lang="scss">
.privacy-policy-container {
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
            
            .sublist {
              margin: 10rpx 0 10rpx 30rpx;
              
              .sublist-item {
                display: block;
                font-size: 24rpx;
                color: #666;
                line-height: 1.5;
                margin-bottom: 8rpx;
                position: relative;
                padding-left: 20rpx;
                
                &::before {
                  content: '○';
                  position: absolute;
                  left: 0;
                  color: #999;
                }
              }
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
