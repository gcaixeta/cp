// Input formatting (for form fields - formats as user types)

export const formatDocument = (value: string) => {
  const numbers = value.replace(/\D/g, "")
  if (numbers.length <= 11) {
    return numbers
      .replace(/(\d{3})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d{1,2})$/, "$1-$2")
  } else {
    return numbers
      .replace(/(\d{2})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d)/, "$1.$2")
      .replace(/(\d{3})(\d)/, "$1/$2")
      .replace(/(\d{4})(\d{1,2})$/, "$1-$2")
  }
}

export const formatPhone = (value: string) => {
  const numbers = value.replace(/\D/g, "")
  if (numbers.length === 11) {
    return numbers.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3")
  } else if (numbers.length === 10) {
    return numbers.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3")
  }
  return numbers
}

export const formatInputCurrency = (value: string) => {
  const numbers = value.replace(/\D/g, "")
  const amount = parseFloat(numbers) / 100
  return amount.toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export const formatInputPercentage = (value: string) => {
  const numbers = value.replace(/\D/g, "")
  if (!numbers) return ""
  const amount = parseFloat(numbers) / 100
  return amount.toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

// Display formatting (for read-only display)

export const formatDisplayDocument = (doc: string) => {
  const numbers = doc.replace(/\D/g, "")
  if (numbers.length === 11) {
    return numbers.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4")
  } else if (numbers.length === 14) {
    return numbers.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, "$1.$2.$3/$4-$5")
  }
  return doc
}

export const formatDisplayPhone = (phone: string | null) => {
  if (!phone) return "---"
  const numbers = phone.replace(/\D/g, "")
  if (numbers.length === 11) {
    return numbers.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3")
  } else if (numbers.length === 10) {
    return numbers.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3")
  }
  return phone
}

export const formatDisplayCurrency = (value: number) => {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value)
}

export const formatDisplayPercentage = (value: number | null) => {
  if (!value) return "---"
  return `${(value * 100).toFixed(2)}%`
}
