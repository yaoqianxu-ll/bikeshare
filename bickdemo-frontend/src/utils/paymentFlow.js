export function createPaymentFlowState() {
  const handledOrders = new Set()
  const confirmingOrders = new Set()
  const checkingOrders = new Set()

  return {
    isHandled(orderNo) {
      return handledOrders.has(orderNo)
    },
    markHandled(orderNo) {
      if (!orderNo || handledOrders.has(orderNo)) {
        return false
      }
      handledOrders.add(orderNo)
      return true
    },
    beginConfirm(orderNo) {
      if (!orderNo || handledOrders.has(orderNo) || confirmingOrders.has(orderNo)) {
        return false
      }
      confirmingOrders.add(orderNo)
      return true
    },
    endConfirm(orderNo) {
      confirmingOrders.delete(orderNo)
    },
    beginStatusCheck(orderNo) {
      if (!orderNo || handledOrders.has(orderNo) || checkingOrders.has(orderNo)) {
        return false
      }
      checkingOrders.add(orderNo)
      return true
    },
    endStatusCheck(orderNo) {
      checkingOrders.delete(orderNo)
    }
  }
}
