<template>
  <!-- 原创「甜心小猫」吉祥物：粉嫩蝴蝶结 + 腮红 + 星星眼，少女卡通风 -->
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    aria-label="甜心小猫"
  >
    <!-- 耳朵 -->
    <path d="M14 22 L10 6 L27 16 Z" fill="#ffffff" stroke="#ffb3cd" stroke-width="1.6" stroke-linejoin="round"/>
    <path d="M50 22 L54 6 L37 16 Z" fill="#ffffff" stroke="#ffb3cd" stroke-width="1.6" stroke-linejoin="round"/>
    <path d="M16.5 19 L13.5 11 L23 16 Z" fill="#ffc9dd"/>
    <path d="M47.5 19 L50.5 11 L41 16 Z" fill="#ffc9dd"/>

    <!-- 头 -->
    <circle cx="32" cy="36" r="22" fill="#ffffff" stroke="#ffb3cd" stroke-width="1.6"/>

    <!-- 左侧耳朵上的蝴蝶结 -->
    <g>
      <path d="M22 8 L14 3 L14 13 Z" fill="#ff8fb3"/>
      <path d="M22 8 L30 3 L30 13 Z" fill="#ff8fb3"/>
      <circle cx="22" cy="8" r="3.2" fill="#f06a96"/>
    </g>

    <!-- 眼睛（带高光） -->
    <ellipse cx="24" cy="35" rx="3.3" ry="4.4" fill="#5a3b4d"/>
    <ellipse cx="40" cy="35" rx="3.3" ry="4.4" fill="#5a3b4d"/>
    <circle cx="25.2" cy="33.4" r="1.2" fill="#ffffff"/>
    <circle cx="41.2" cy="33.4" r="1.2" fill="#ffffff"/>

    <!-- 腮红 -->
    <circle cx="18.5" cy="42" r="3.4" fill="#ffc2d6" opacity="0.85"/>
    <circle cx="45.5" cy="42" r="3.4" fill="#ffc2d6" opacity="0.85"/>

    <!-- 鼻子 + 嘴 -->
    <path d="M30 40 h4 l-2 2.4 z" fill="#ff8fb3"/>
    <path d="M32 42.4 q-3 3 -6 1.2 M32 42.4 q3 3 6 1.2" stroke="#caa0b4" stroke-width="1.3" fill="none" stroke-linecap="round"/>
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 28 }
})
</script>
