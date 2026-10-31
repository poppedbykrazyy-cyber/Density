import { atom, read, update } from 'claude-code'
import type { Register } from 'claude-code'

// Palette
const BLACK = '#0a0612'
const PANEL = '#140b24'
const PURPLE = '#8b5cf6'
const VIOLET = '#c084fc'
const LAVENDER = '#e9d5ff'

const isOn = atom({ plugin: 'midnight-purple', key: 'isOn' } as const, true)

export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    await $.command.register({
      name: 'purple',
      description: 'Toggle the black & purple theme on or off',
    })
    return next(e)
  })

  on('command.run', { command: 'purple' }, async $ => {
    const now = await update($, isOn, v => !v)
    $.ui.toast(now ? '◆ Midnight Purple on' : 'Midnight Purple off')
    return { text: `Midnight Purple theme ${now ? 'enabled' : 'disabled'}.` }
  })

  // Your messages: deep purple card with a violet border
  on('ui.render', { component: 'UserMessage' }, async ($, e, next) => {
    if (!(await read($, isOn))) return next(e)
    const { Box } = $.ui.resolve(e)
    return (
      <Box borderStyle="round" borderColor={PURPLE} backgroundColor={PANEL} paddingLeft={1} paddingRight={1}>
        {await next(e)}
      </Box>
    )
  })

  // Claude's replies: near-black with a purple left rule
  on('ui.render', { component: 'AssistantMessage' }, async ($, e, next) => {
    if (!(await read($, isOn))) return next(e)
    const { Box } = $.ui.resolve(e)
    return (
      <Box borderStyle="round" borderColor={VIOLET} borderDimColor backgroundColor={BLACK} paddingLeft={1}>
        {await next(e)}
      </Box>
    )
  })

  // Tool rows: black with a dim purple frame
  on('ui.render', { component: 'ToolUse' }, async ($, e, next) => {
    if (!(await read($, isOn))) return next(e)
    const { Box } = $.ui.resolve(e)
    return (
      <Box borderStyle="round" borderColor={PURPLE} borderDimColor backgroundColor={BLACK} paddingLeft={1}>
        {await next(e)}
      </Box>
    )
  })

  // Spinner: violet diamond in front of the working line
  on('ui.render', { component: 'Spinner' }, async ($, e, next) => {
    if (!(await read($, isOn))) return next(e)
    const { Box, Text } = $.ui.resolve(e)
    return (
      <Box backgroundColor={BLACK}>
        <Text color={VIOLET} bold>◆ </Text>
        {await next(e)}
      </Box>
    )
  })

  // A slim band above the prompt
  on('ui.render', { component: 'AbovePrompt' }, async ($, e, next) => {
    if (e.props.hasSurvey || !(await read($, isOn))) return next(e)
    const { Box, Text } = $.ui.resolve(e)
    return (
      <Box backgroundColor={BLACK} paddingLeft={1}>
        <Text color={PURPLE} bold>◆ </Text>
        <Text color={LAVENDER} backgroundColor={BLACK}>
          {e.props.isWorking ? 'Claude is working…' : 'midnight purple'}
        </Text>
        <Text color={VIOLET} dimColor>  /purple to toggle</Text>
      </Box>
    )
  })
}
