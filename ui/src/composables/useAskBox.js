import { ref, reactive, computed } from "vue";

// ==================== Avatar Options ====================
export const avatarOptions = [
  { icon: "ri-user-smile-line", bg: "#4f46e5", name: "好奇的猫" },
  { icon: "ri-footprint-line", bg: "#64748b", name: "路过的狐狸" },
  { icon: "ri-moon-line", bg: "#7c3aed", name: "深夜访客" },
  { icon: "ri-star-line", bg: "#f59e0b", name: "月亮兔" },
  { icon: "ri-leaf-line", bg: "#10b981", name: "森林旅人" },
  { icon: "ri-user-line", bg: "#6b7280", name: "发呆的熊猫" },
  { icon: "ri-rocket-line", bg: "#2563eb", name: "追星星的人" },
  { icon: "ri-eye-line", bg: "#8b5cf6", name: "不眠的猫头鹰" },
  { icon: "ri-drop-line", bg: "#06b6d4", name: "深海的鲸" },
  { icon: "ri-sun-line", bg: "#f97316", name: "窗边的向日葵" },
];

// ==================== State ====================
const publishedQA = reactive([
  {
    id: "q1",
    profile: avatarOptions[0],
    question: "你当初是怎么决定进入这个行业的？有没有过犹豫和后悔的时刻？",
    answer:
      "说实话，犹豫过无数次。大学学的不是这个方向，毕业后做了一年完全不相关的工作。转折点是有天晚上自己写了个小程序解决了一个很小的痛点，然后突然意识到——原来我是真的喜欢创造东西的感觉。后悔？从来没有。但迷茫常常有，这是实话。",
    time: "2天前",
    ts: Date.now() - 2 * 86400000,
  },
  {
    id: "q2",
    profile: avatarOptions[1],
    question: "平时会感到孤独吗？自己一个人呆着的时候都在做什么？",
    answer:
      "会。大概没有人从来不孤独。独处的时候我喜欢整理书架、煮咖啡、或者出门走很长很长的路。最近发现听播客是个很好的陪伴方式，仿佛有人在耳边聊天，但又不用社交。",
    time: "3天前",
    ts: Date.now() - 3 * 86400000,
  },
  {
    id: "q3",
    profile: avatarOptions[2],
    question: "推荐三本对你影响最大的书吧，什么领域都可以。",
    answer:
      "《斯通纳》——它教会我平凡人生的尊严感；《思考，快与慢》——让我理解自己的大脑有多不可靠；《活着》——读完之后沉默了很久，对生活有了完全不同的理解。",
    time: "5天前",
    ts: Date.now() - 5 * 86400000,
  },
  {
    id: "q4",
    profile: avatarOptions[3],
    question: "如果有人问你一个你不想回答的问题，你会怎么处理？",
    answer:
      '我会直接说"这个问题我想保留一点"。坦诚地拒绝比敷衍地回答更尊重对方，也更尊重自己。不过话说回来，在我自己的提问箱里——我会尽量回答每一个问题，只是有些问题可能答得比较简短。',
    time: "1周前",
    ts: Date.now() - 7 * 86400000,
  },
  {
    id: "q5",
    profile: avatarOptions[4],
    question: "最近让你感到开心的一件小事是什么？",
    answer:
      "三天前的傍晚，下楼拿快递的时候看到晚霞特别美，就在小区的长椅上坐了十分钟，什么都不想，就看着天色一点一点暗下去。那十分钟仿佛偷来的，特别奢侈。",
    time: "1周前",
    ts: Date.now() - 8 * 86400000,
  },
  {
    id: "q6",
    profile: avatarOptions[5],
    question: "你是怎么保持学习和成长的动力的？有没有什么具体的方法？",
    answer:
      "好奇心是我的主要燃料。我从不强迫自己「学习」，而是追随自己感兴趣的东西。具体方法：每天至少留30分钟无目的阅读时间，不是工作相关的，纯粹自己好奇的。另外，写作也是很好的学习方式——你得弄清楚才能写清楚。",
    time: "2周前",
    ts: Date.now() - 14 * 86400000,
  },
  {
    id: "q7",
    profile: avatarOptions[6],
    question: "如果现在给你一整年的自由时间，不用为钱发愁，你会去做什么？",
    answer:
      "我会从头到尾学一门全新的领域，可能是天文学或者陶艺。然后花三个月时间在一个语言不通的小城市生活，用最笨的方式学会日常交流。剩下的时间，我想写一本不着急出版的小说。",
    time: "2周前",
    ts: Date.now() - 15 * 86400000,
  },
]);

const pendingQuestions = reactive([
  {
    id: "p1",
    profile: avatarOptions[7],
    question:
      '你怎么看待现在大家都在讨论的「内卷」问题？作为个体我们能做什么？',
    time: "1天前",
    ts: Date.now() - 86400000,
  },
  {
    id: "p2",
    profile: avatarOptions[8],
    question: "你觉得自己是一个勇敢的人吗？为什么？",
    time: "6小时前",
    ts: Date.now() - 6 * 3600000,
  },
]);

const isAdmin = ref(false);
const selectedAvatar = ref(null);
const visibleCount = ref(publishedQA.length);
const justPublishedId = ref(null);

// ==================== Helpers ====================
export function formatTime(ts) {
  const diff = Date.now() - ts;
  const m = Math.floor(diff / 60000);
  const h = Math.floor(diff / 3600000);
  const d = Math.floor(diff / 86400000);
  const w = Math.floor(d / 7);
  if (m < 1) return "刚刚";
  if (m < 60) return `${m}分钟前`;
  if (h < 24) return `${h}小时前`;
  if (d < 7) return `${d}天前`;
  if (w < 4) return `${w}周前`;
  return `${Math.floor(d / 30)}个月前`;
}

// ==================== Composable ====================
export function useAskBox() {
  // Init
  if (selectedAvatar.value === null) {
    const idx = Math.floor(Math.random() * avatarOptions.length);
    selectedAvatar.value = avatarOptions[idx];
  }
  if (isAdmin.value === false && typeof window !== "undefined") {
    isAdmin.value = window.location.search.includes("admin");
  }
  if (visibleCount.value === 0) {
    visibleCount.value = publishedQA.length;
  }

  return {
    avatarOptions,
    publishedQA,
    pendingQuestions,
    isAdmin,
    selectedAvatar,
    visibleCount,
    justPublishedId,

    // Actions
    selectAvatar(avatar) {
      selectedAvatar.value = avatar;
    },

    async sendQuestion(content) {
      const profile = selectedAvatar.value;
      await new Promise((r) => setTimeout(r, 700 + Math.random() * 500));

      if (isAdmin.value) {
        pendingQuestions.push({
          id: "p" + Date.now(),
          profile,
          question: content,
          time: "刚刚",
          ts: Date.now(),
        });
      }

      return profile;
    },

    publishAnswer(id, answer) {
      const idx = pendingQuestions.findIndex((q) => q.id === id);
      if (idx === -1) return null;
      const pq = pendingQuestions[idx];
      justPublishedId.value = pq.id;
      publishedQA.unshift({
        id: pq.id,
        profile: pq.profile,
        question: pq.question,
        answer,
        time: "刚刚",
        ts: Date.now(),
      });
      pendingQuestions.splice(idx, 1);
      setTimeout(() => {
        justPublishedId.value = null;
      }, 1200);
      return pq.profile;
    },

    dismissQuestion(id) {
      const idx = pendingQuestions.findIndex((q) => q.id === id);
      if (idx === -1) return;
      pendingQuestions.splice(idx, 1);
    },

    loadMore() {
      visibleCount.value = Math.min(
        visibleCount.value + 4,
        publishedQA.length,
      );
    },

    hasMore: computed(
      () => visibleCount.value < publishedQA.length,
    ),
  };
}
