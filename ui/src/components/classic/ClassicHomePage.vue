<script setup>
import { onActivated, onDeactivated } from 'vue'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['activated', 'deactivated'])

onActivated(() => {
  emit('activated', props.name)
})

onDeactivated(() => {
  emit('deactivated', props.name)
})
</script>

<template>
  <section>
    <slot />
  </section>
</template>
