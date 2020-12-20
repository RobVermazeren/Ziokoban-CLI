package nl.itvanced.ziokoban.error

trait ZiokobanError extends Throwable {
  def message: String
}